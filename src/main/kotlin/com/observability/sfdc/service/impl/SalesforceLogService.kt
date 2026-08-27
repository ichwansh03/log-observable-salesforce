package com.observability.sfdc.service.impl

import com.observability.sfdc.dto.*
import com.observability.sfdc.exception.ResourceNotFoundException
import com.observability.sfdc.exception.SalesforceApiException
import com.observability.sfdc.exception.ValidationException
import com.observability.sfdc.repository.DebugLevelRepository
import com.observability.sfdc.repository.LogRepository
import com.observability.sfdc.service.LogService
import com.observability.sfdc.service.SalesforceBaseService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class SalesforceLogService(
    authService: SalesforceAuthService,
    private val debugLevelRepository: DebugLevelRepository,
    private val logRepository: LogRepository,
    private val minioService: MinioService,
    @Value($$"${salesforce.api-version}") apiVersion: String
) : SalesforceBaseService(authService, apiVersion), LogService {
    private val salesforceIdRegex = Regex("^[a-zA-Z0-9]{15}(?:[a-zA-Z0-9]{3})?$")

    init {
        restTemplate.requestFactory = JdkClientHttpRequestFactory()
    }

    private fun isValidSalesforceId(id: String): Boolean = salesforceIdRegex.matches(id)
    private val sfdcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    override fun queryApexLogs(limit: Int, offset: Int, fetchBody: Boolean): List<ApexLogDto> {
        val query = "SELECT Id, LogUser.Name, Operation, StartTime, Status, Request, LogLength, DurationMilliseconds FROM ApexLog ORDER BY StartTime DESC LIMIT $limit OFFSET $offset"
        val records = querySalesforce("querying ApexLogs", query, object : ParameterizedTypeReference<SalesforceQueryResult<ApexLogDto>>() {}, useTooling = false)

        if (!fetchBody || records.isEmpty()) return records

        // Enrich records with Apex Class Name by checking DB first, then fetching body from MinIO
        return records.map { dto ->
            val dbLog = logRepository.findBySfdcId(dto.id)
            val className = dbLog.get().apexClassName ?: run {
                val body = getLogBody(dto.id)
                extractClassName(body)
            }
            dto.copy(apexClassName = className)
        }
    }

    override fun extractClassName(body: String?): String? {
        if (body == null) return null

        // Pattern to find the last CODE_UNIT_STARTED or CODE_UNIT_FINISHED which contains the entry point.
        // This is more reliable as it's typically at the end of the log and captures full context (Classes, Triggers, VF).
        val codeUnitRegex = Regex("\\|CODE_UNIT_(?:STARTED|FINISHED)\\|(?:.*\\|)?([^\\r\\n|]+)")
        val matches = codeUnitRegex.findAll(body).toList()

        if (matches.isNotEmpty()) {
            val fullPath = matches.last().groupValues[1].trim()

            // Handle Visualforce pages: VF: /apex/PageName -> extract PageName
            if (fullPath.startsWith("VF: /apex/")) return fullPath.substringAfterLast("/")

            // Handle Internal Triggers: __sfdc_trigger/TriggerName -> extract TriggerName
            if (fullPath.startsWith("__sfdc_trigger/")) return fullPath.substringAfter("/")

            // Handle Triggers: TriggerName on SObject -> keep full trigger context
            if (fullPath.contains(" on ", ignoreCase = true)) return fullPath

            // Handle Apex Classes: ClassName.methodName -> extract only ClassName
            // Taking the part before the last dot as the metadata name (handles namespaces correctly).
            return fullPath.substringBeforeLast(".")
        }

        // Fallback for standard Apex classes if no CODE_UNIT info is found
        val classRegex = Regex("\\|(?:METHOD_ENTRY|CLASS_ENTRY)\\|\\[[^]]*]\\|(?:[^|]*\\|)?([^.| \\n]+)")
        return classRegex.find(body)?.groupValues?.get(1)
    }

    override fun getLogBody(logId: String): String? {
        if (!isValidSalesforceId(logId)) throw ValidationException("Invalid Salesforce ID: $logId", "logId")

        // 1. Try MinIO first (compressed)
        val cachedBody = minioService.downloadLog(logId)
        if (cachedBody != null) return cachedBody

        // 2. Fallback to Salesforce Tooling API
        val body = executeWithToken("fetching log body for $logId from Salesforce", null) { token, instanceUrl ->
            val url = buildUri(instanceUrl, "sobjects/ApexLog/$logId/Body").build().toUriString()
            restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Unit>(createHeaders(token)), String::class.java).body
        }

        // 3. Store in MinIO for future use
        if (body != null) minioService.uploadLog(logId, body)
        
        return body
    }

    override fun getLogDownloadStream(logId: String): InputStream? {
        // Ensure log exists in MinIO first
        if (!minioService.exists(logId)) {
            val body = getLogBody(logId) ?: return null // This will fetch from SFDC
            minioService.uploadLogSync(logId, body)
        }
        return minioService.getDownloadStream(logId)
    }

    override fun createTraceFlag(frontendRequest: FrontendTraceFlagRequest): SalesforceCreateResponse? {
        // Resolve DebugLevel ID
        val debugLevels = debugLevelRepository.findAll()
        val debugLevel = debugLevels.find { it.developerName == frontendRequest.debugLevelName || it.masterLabel == frontendRequest.debugLevelName }
            ?: throw ResourceNotFoundException(
                "DebugLevel '${frontendRequest.debugLevelName}' not found. Please sync metadata first.",
                "DebugLevel",
                frontendRequest.debugLevelName
            )

        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val startDate = now.format(sfdcFormatter)
        val expirationDate = now
            .plusDays((frontendRequest.durationDays ?: 0).toLong())
            .plusHours((frontendRequest.durationHours ?: 0).toLong())
            .plusMinutes((frontendRequest.durationMinutes ?: 0).toLong())
            .format(sfdcFormatter)

        val logType = when (frontendRequest.entityType) {
            "ApexClass", "ApexTrigger" -> "CLASS_TRACING"
            else -> "USER_DEBUG"
        }

        val sfdcRequest = TraceFlagRequest(
            tracedEntityId = frontendRequest.tracedEntityId,
            debugLevelId = debugLevel.sfdcId,
            logType = logType,
            startDate = startDate,
            expirationDate = expirationDate
        )

        return executeWithToken("creating TraceFlag", null) { token, instanceUrl ->
            val url = buildUri(instanceUrl, "sobjects/TraceFlag").build().toUriString()
            val entity = HttpEntity(sfdcRequest, createHeaders(token, MediaType.APPLICATION_JSON))
            restTemplate.postForObject(url, entity, SalesforceCreateResponse::class.java)
        } ?: throw SalesforceApiException("Failed to create TraceFlag — authentication error", "createTraceFlag")
    }

    override fun getActiveTraceFlags(): List<TraceFlagDto> {
        val now = ZonedDateTime.now(ZoneId.of("UTC")).format(sfdcFormatter)
        val query = "SELECT Id, TracedEntityId, TracedEntity.Name, StartDate, ExpirationDate, DebugLevelId, DebugLevel.DeveloperName, LogType FROM TraceFlag WHERE ExpirationDate > $now"
        return querySalesforce("querying active TraceFlags", query, object : ParameterizedTypeReference<SalesforceQueryResult<TraceFlagDto>>() {})
    }

    override fun getAllTraceFlags(): List<TraceFlagDto> {
        val query = "SELECT Id, TracedEntityId, TracedEntity.Name, StartDate, ExpirationDate, DebugLevelId, DebugLevel.DeveloperName, LogType FROM TraceFlag ORDER BY ExpirationDate DESC"
        return querySalesforce("querying all TraceFlags", query, object : ParameterizedTypeReference<SalesforceQueryResult<TraceFlagDto>>() {})
    }

    @Transactional
    override fun deleteLog(id: String): Boolean {
        if (!isValidSalesforceId(id)) throw ValidationException("Invalid Salesforce ID: $id", "id")
        
        // 1. Delete from Salesforce
        val deletedFromSF = executeWithToken("deleting ApexLog $id", false) { token, instanceUrl ->
            val uri = buildUri(instanceUrl, "sobjects/ApexLog/$id", useTooling = false).build().toUri()
            restTemplate.exchange(uri, HttpMethod.DELETE, HttpEntity<Unit>(createHeaders(token)), Unit::class.java).statusCode.is2xxSuccessful
        }

        // 2. Cleanup local storage and database
        minioService.deleteLog(id)
        logRepository.deleteBySfdcId(id)
        
        return deletedFromSF
    }

    @Transactional
    override fun deleteLogs(ids: List<String>): Map<String, Boolean> {
        return ids.associateWith { deleteLog(it) }
    }

    @Transactional
    override fun deleteAllLogs(): Int {
        val query = "SELECT Id FROM ApexLog"
        val records = querySalesforce("querying all ApexLogs for deletion", query, object : ParameterizedTypeReference<SalesforceQueryResult<ApexLogDto>>() {}, useTooling = false)
        
        var count = 0
        records.forEach { 
            if (deleteLog(it.id)) count++
        }

        return count
    }

    override fun deleteTraceFlag(id: String): Boolean {
        if (!isValidSalesforceId(id)) throw ValidationException("Invalid Salesforce ID: $id", "id")
        
        return executeWithToken("deleting TraceFlag $id", false) { token, instanceUrl ->
            val uri = buildUri(instanceUrl, "sobjects/TraceFlag/$id").build().toUri()
            restTemplate.exchange(uri, HttpMethod.DELETE, HttpEntity<Unit>(createHeaders(token)), Unit::class.java).statusCode.is2xxSuccessful
        }
    }

    override fun patchTraceFlag(id: String, startDate: String, expirationDate: String): Boolean {
        if (!isValidSalesforceId(id)) throw ValidationException("Invalid Salesforce ID: $id", "id")
        
        return executeWithToken("patching TraceFlag $id", false) { token, instanceUrl ->
            val uri = buildUri(instanceUrl, "sobjects/TraceFlag/$id").build().toUri()
            val body = mapOf(
                "StartDate" to startDate,
                "ExpirationDate" to expirationDate
            )
            val entity = HttpEntity(body, createHeaders(token, MediaType.APPLICATION_JSON))
            
            // Note: RestTemplate requires a specific RequestFactory (like JdkClientHttpRequestFactory) to support PATCH.
            restTemplate.exchange(uri, HttpMethod.PATCH, entity, Unit::class.java).statusCode.is2xxSuccessful
        }
    }
}
