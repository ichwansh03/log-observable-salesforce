package com.observability.sfdc.service.impl

import com.observability.sfdc.domain.ApexClass
import com.observability.sfdc.domain.ApexTrigger
import com.observability.sfdc.domain.DebugLevel
import com.observability.sfdc.domain.MetadataHistory
import com.observability.sfdc.domain.Report
import com.observability.sfdc.dto.*
import com.observability.sfdc.exception.ResourceNotFoundException
import com.observability.sfdc.exception.ValidationException
import com.observability.sfdc.repository.ApexClassRepository
import com.observability.sfdc.repository.ApexTriggerRepository
import com.observability.sfdc.repository.DebugLevelRepository
import com.observability.sfdc.repository.MetadataHistoryRepository
import com.observability.sfdc.repository.ReportRepository
import com.observability.sfdc.service.MetadataService
import com.observability.sfdc.util.ReportToSoqlConverter
import com.observability.sfdc.service.SalesforceBaseService
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SalesforceMetadataService(
    authService: SalesforceAuthService,
    private val classRepository: ApexClassRepository,
    private val triggerRepository: ApexTriggerRepository,
    private val debugLevelRepository: DebugLevelRepository,
    private val metadataHistoryRepository: MetadataHistoryRepository,
    private val reportRepository: ReportRepository,
    private val reportToSoqlConverter: ReportToSoqlConverter,
    @Value($$"${salesforce.api-version}") apiVersion: String
    ) : SalesforceBaseService(authService, apiVersion), MetadataService {

    private val salesforceIdPattern = Regex("^[a-zA-Z0-9]{15}(?:[a-zA-Z0-9]{3})?$")

    @Cacheable(value = ["sf_metadata"], key = "'debug_levels_' + (#name ?: 'all') + '_' + #limit + '_' + #offset", unless = "#result == null")
    @Transactional
    override fun getAllDebugLevels(name: String?, limit: Int, offset: Int): List<DebugLevelDto> {
        var query = "SELECT Id, DeveloperName, MasterLabel, ApexCode, ApexProfiling, Callout, Database, System, Validation, Visualforce, Workflow FROM DebugLevel "
        if (!name.isNullOrBlank()) {
            val escapedName = name.replace("'", "\\'")
            query += "WHERE DeveloperName LIKE '%$escapedName%' OR MasterLabel LIKE '%$escapedName%' "
        }
        query += "LIMIT $limit OFFSET $offset"
        
        val records = querySalesforce("querying DebugLevels", query, object : ParameterizedTypeReference<SalesforceQueryResult<DebugLevelDto>>() {})
        if (records.isNotEmpty()) syncDebugLevelsToDatabase(records)
        return records
    }

    override fun fetchApexClassesFromSalesforce(name: String?, limit: Int, offset: Int): List<ApexClassDto> {
        var query = "SELECT Id, Name, ApiVersion, Status, LengthWithoutComments, LastModifiedDate, LastModifiedBy.Name, CreatedDate, CreatedBy.Name, Body FROM ApexClass WHERE Status = 'Active' "
        if (!name.isNullOrBlank()) {
            val escapedName = name.replace("'", "\\'")
            query += "AND Name LIKE '%$escapedName%' "
        }
        
        query += "AND (NOT Name LIKE '%Test') AND (NOT Name LIKE 'Test%') AND (NOT Name LIKE '%Tests') AND (NOT Name LIKE '%Mock') AND (NOT Name LIKE '%Factory') "
        query += "ORDER BY Name ASC LIMIT $limit OFFSET $offset"
        
        val records = querySalesforce("querying ApexClasses", query, object : ParameterizedTypeReference<SalesforceQueryResult<ApexClassDto>>() {})
        if (records.isNotEmpty()) {
            val coverageMap = fetchCoverageForMetadata(records.map { it.id })
            return records.map { it.copy(coverage = coverageMap[it.id]) }
        }
        return records
    }

    override fun fetchApexTriggersFromSalesforce(name: String?, limit: Int, offset: Int): List<ApexTriggerDto> {
        var query = "SELECT Id, Name, TableEnumOrId, ApiVersion, Status, UsageBeforeInsert, UsageBeforeUpdate, UsageBeforeDelete, UsageAfterInsert, UsageAfterUpdate, UsageAfterDelete, UsageAfterUndelete, LastModifiedDate, LastModifiedBy.Name, CreatedDate, CreatedBy.Name, Body FROM ApexTrigger WHERE Status = 'Active' "
        if (!name.isNullOrBlank()) {
            val escapedName = name.replace("'", "\\'")
            query += "AND Name LIKE '%$escapedName%' "
        }
        query += "ORDER BY Name ASC LIMIT $limit OFFSET $offset"
        
        val records = querySalesforce("querying ApexTriggers", query, object : ParameterizedTypeReference<SalesforceQueryResult<ApexTriggerDto>>() {})
        if (records.isNotEmpty()) {
            val coverageMap = fetchCoverageForMetadata(records.map { it.id })
            return records.map { it.copy(coverage = coverageMap[it.id]) }
        }
        return records
    }

    @Cacheable(value = ["sf_metadata"], key = "'apex_classes_' + (#name ?: 'all') + '_' + #limit + '_' + #offset", unless = "#result == null")
    override fun getAllApexClasses(name: String?, limit: Int, offset: Int): List<ApexClassDto> = fetchApexClassesFromSalesforce(name, limit, offset)

    @Cacheable(value = ["sf_metadata"], key = "'apex_triggers_' + (#name ?: 'all') + '_' + #limit + '_' + #offset", unless = "#result == null")
    override fun getAllApexTriggers(name: String?, limit: Int, offset: Int): List<ApexTriggerDto> = fetchApexTriggersFromSalesforce(name, limit, offset)

    @Cacheable(value = ["sf_metadata"], key = "'reports_' + (#name ?: 'all') + '_' + #limit + '_' + #offset", unless = "#result == null")
    override fun getAllReports(name: String?, limit: Int, offset: Int): List<ReportDto> {
        var query = "SELECT Id, Name, DeveloperName, FolderName, CreatedDate, CreatedBy.Name, LastModifiedDate, LastModifiedBy.Name FROM Report "
        if (!name.isNullOrBlank()) {
            val escapedName = name.replace("'", "\\'")
            query += "WHERE Name LIKE '%$escapedName%' OR DeveloperName LIKE '%$escapedName%' "
        }
        query += "ORDER BY Name ASC LIMIT $limit OFFSET $offset"

        val records = querySalesforce("querying Reports", query, object : ParameterizedTypeReference<SalesforceQueryResult<ReportDto>>() {}, useTooling = false)
        if (records.isNotEmpty()) syncReportsToDatabase(records)
        return records
    }

    @Cacheable(value = ["sf_metadata"], key = "'report_describe_' + #reportId", unless = "#result == null")
    override fun getReportDescribe(reportId: String): ReportDescribeDto? {
        val safeId = reportId.trim().replace("'", "\\'")
        return executeWithToken("fetching report describe for $safeId", null) { token, instanceUrl ->
            val uri = "$instanceUrl/services/data/$apiVersion/analytics/reports/$safeId/describe"
            val entity = HttpEntity<Unit>(createHeaders(token))
            val response = restTemplate.exchange(uri, HttpMethod.GET, entity, ReportDescribeDto::class.java)
            response.body
        }
    }

    override fun convertReportToSoql(reportId: String): ReportSoqlDto? {
        val describe = getReportDescribe(reportId) ?: return null
        return reportToSoqlConverter.convert(reportId, describe)
    }


    private fun fetchCoverageForMetadata(ids: List<String>): Map<String, ApexCodeCoverageDto> {
        if (ids.isEmpty()) return emptyMap()
        val idList = ids.joinToString(",") { "'$it'" }
        val query = "SELECT ApexClassOrTriggerId, NumLinesCovered, NumLinesUncovered FROM ApexCodeCoverageAggregate WHERE ApexClassOrTriggerId IN ($idList)"
        return querySalesforce("querying coverage", query, object : ParameterizedTypeReference<SalesforceQueryResult<ApexCodeCoverageDto>>() {})
            .associateBy { it.apexClassOrTriggerId }
    }

    // --- Search methods ---
    override fun searchClasses(name: String?, limit: Int, offset: Int): List<ApexClass> {
        val pageable = PageRequest.of(offset / limit, limit, Sort.by("name").ascending())
        if (!name.isNullOrBlank()) {
            val dtos = fetchApexClassesFromSalesforce(name, 200, 0)
            syncClassesToDatabase(dtos)
        } else if (classRepository.count() == 0L) {
            val dtos = fetchApexClassesFromSalesforce(null, 200, 0)
            syncClassesToDatabase(dtos)
        }
        return if (name.isNullOrBlank()) classRepository.findAllProjectedBy(pageable) else classRepository.findByNameContainingIgnoreCase(name, pageable)
    }

    override fun searchTriggers(name: String?, limit: Int, offset: Int): List<ApexTrigger> {
        val pageable = PageRequest.of(offset / limit, limit, Sort.by("name").ascending())
        if (!name.isNullOrBlank()) {
            val dtos = fetchApexTriggersFromSalesforce(name, 200, 0)
            syncTriggersToDatabase(dtos)
        } else if (triggerRepository.count() == 0L) {
            val dtos = fetchApexTriggersFromSalesforce(null, 200, 0)
            syncTriggersToDatabase(dtos)
        }
        return if (name.isNullOrBlank()) triggerRepository.findAllProjectedBy(pageable) else triggerRepository.findByNameContainingIgnoreCaseOrSobjectContainingIgnoreCase(name, name, pageable)
    }

    override fun searchDebugLevels(name: String?, limit: Int, offset: Int): List<DebugLevel> {
        val pageable = PageRequest.of(offset / limit, limit, Sort.by("developerName").ascending())
        if (!name.isNullOrBlank()) getAllDebugLevels(name, 200, 0) else if (debugLevelRepository.count() == 0L) getAllDebugLevels(null, 200, 0)
        return if (name.isNullOrBlank()) debugLevelRepository.findAllProjectedBy(pageable) else debugLevelRepository.findByDeveloperNameContainingIgnoreCaseOrMasterLabelContainingIgnoreCase(name, name, pageable)
    }

    override fun searchReports(name: String?, limit: Int, offset: Int): List<Report> {
        val pageable = PageRequest.of(offset / limit, limit, Sort.by("name").ascending())
        if (!name.isNullOrBlank()) {
            val dtos = getAllReports(name, 200, 0)
            syncReportsToDatabase(dtos)
        } else if (reportRepository.count() == 0L) {
            val dtos = getAllReports(null, 200, 0)
            syncReportsToDatabase(dtos)
        }
        return if (name.isNullOrBlank()) reportRepository.findAllProjectedBy(pageable) else reportRepository.findByNameContainingIgnoreCaseOrDeveloperNameContainingIgnoreCase(name, name, pageable)
    }

    // --- Detail & Related ---
    override fun getMetadataDetail(id: String, type: String): MetadataDetailDto? {
        val objectType = if (type == "ApexClass" || type == "ApexTrigger") type else throw ValidationException("Invalid metadata type: $type. Must be 'ApexClass' or 'ApexTrigger'", "type")
        if (!salesforceIdPattern.matches(id)) throw ValidationException("Invalid Salesforce ID: $id", "id")
        val fields = if (objectType == "ApexTrigger") "Id, Name, TableEnumOrId, ApiVersion, Status, UsageBeforeInsert, UsageBeforeUpdate, UsageBeforeDelete, UsageAfterInsert, UsageAfterUpdate, UsageAfterDelete, UsageAfterUndelete, LastModifiedDate, LastModifiedBy.Name, Body"
                     else "Id, Name, ApiVersion, Status, LastModifiedDate, LastModifiedBy.Name, Body"
        
        val safeId = id.trim().replace("'", "\\'")
        val query = "SELECT $fields FROM $objectType WHERE Id = '$safeId'"
        return executeWithToken("fetching metadata detail for $id", null) { token, instanceUrl ->
            val uri = buildUri(instanceUrl, "query").queryParam("q", query).build().toUri()
            val coverage = fetchCoverageForMetadata(listOf(safeId))[safeId]
            
            if (objectType == "ApexTrigger") {
                val trigger = restTemplate.exchange(uri, HttpMethod.GET, HttpEntity<Unit>(createHeaders(token)), object : ParameterizedTypeReference<SalesforceQueryResult<ApexTriggerDto>>() {}).body?.records?.firstOrNull() ?: return@executeWithToken null
                MetadataDetailDto(trigger.id, trigger.name!!, "ApexTrigger", trigger.apiVersion, trigger.status, trigger.lastModifiedDate, trigger.lastModifiedBy?.name, trigger.tableEnumOrId, mapTriggerEvents(trigger), findRelatedTestClasses(
                    trigger.name
                ), coverage, body = trigger.body)
            } else {
                val apexClass = restTemplate.exchange(uri, HttpMethod.GET, HttpEntity<Unit>(createHeaders(token)), object : ParameterizedTypeReference<SalesforceQueryResult<ApexClassDto>>() {}).body?.records?.firstOrNull() ?: return@executeWithToken null
                MetadataDetailDto(apexClass.id, apexClass.name!!, "ApexClass", apexClass.apiVersion, apexClass.status, apexClass.lastModifiedDate, apexClass.lastModifiedBy?.name, testClasses = findRelatedTestClasses(
                    apexClass.name
                ), coverage = coverage, body = apexClass.body)
            }
        }
    }

    internal open fun findRelatedTestClasses(name: String): List<ApexClassDto> {
        return executeWithToken("searching related test classes for $name", emptyList()) { token, instanceUrl ->
            val safeName = name.replace("'", "\\'")
            val sosl = "FIND {$safeName AND \"@isTest\"} IN ALL FIELDS RETURNING ApexClass (Id, Name, ApiVersion, Status, LastModifiedDate, LastModifiedBy.Name WHERE Name != '$safeName' AND Status = 'Active')"
            val uri = buildUri(instanceUrl, "search").queryParam("q", sosl).build().toUri()
            restTemplate.exchange(uri, HttpMethod.GET, HttpEntity<Unit>(createHeaders(token)), object : ParameterizedTypeReference<SalesforceSearchResponse<ApexClassDto>>() {}).body?.searchRecords ?: emptyList()
        }
    }

    // --- Sync Methods ---
    override fun syncDebugLevelsToDatabase(dtos: List<DebugLevelDto>) = dtos.distinctBy { it.id }.forEach { dto ->
        val entity = debugLevelRepository.findBySfdcId(dto.id).orElse(DebugLevel(sfdcId = dto.id, developerName = dto.developerName, masterLabel = dto.masterLabel, apexCode = dto.apexCode, apexProfiling = dto.apexProfiling, callout = dto.callout, database = dto.database, system = dto.system, validation = dto.validation, visualforce = dto.visualforce, workflow = dto.workflow))
        debugLevelRepository.save(entity.copy(developerName = dto.developerName, masterLabel = dto.masterLabel, apexCode = dto.apexCode, apexProfiling = dto.apexProfiling, callout = dto.callout, database = dto.database, system = dto.system, validation = dto.validation, visualforce = dto.visualforce, workflow = dto.workflow))
    }

    override fun syncClassesToDatabase(dtos: List<ApexClassDto>) = dtos.distinctBy { it.id }.forEach { dto ->
        val entity = classRepository.findBySfdcId(dto.id).orElse(ApexClass(sfdcId = dto.id, name = dto.name, apiVersion = dto.apiVersion, status = dto.status, lengthWithoutComments = dto.lengthWithoutComments, lastModifiedDate = dto.lastModifiedDate, lastModifiedByName = dto.lastModifiedBy?.name, createdDate = dto.createdDate, createdByName = dto.createdBy?.name, numLinesCovered = dto.coverage?.numLinesCovered, numLinesUncovered = dto.coverage?.numLinesUncovered, body = dto.body))
        if (entity.body != null && entity.body != dto.body) {
            metadataHistoryRepository.save(MetadataHistory(sfdcId = dto.id, entityType = "ApexClass", body = entity.body))
        }
        classRepository.save(entity.copy(name = dto.name, apiVersion = dto.apiVersion, status = dto.status, lengthWithoutComments = dto.lengthWithoutComments, lastModifiedDate = dto.lastModifiedDate, lastModifiedByName = dto.lastModifiedBy?.name, createdDate = dto.createdDate, createdByName = dto.createdBy?.name, numLinesCovered = dto.coverage?.numLinesCovered, numLinesUncovered = dto.coverage?.numLinesUncovered, body = dto.body))
    }

    override fun syncTriggersToDatabase(dtos: List<ApexTriggerDto>) = dtos.distinctBy { it.id }.forEach { dto ->
        val entity = triggerRepository.findBySfdcId(dto.id).orElse(ApexTrigger(sfdcId = dto.id, name = dto.name, sobject = dto.tableEnumOrId, apiVersion = dto.apiVersion, status = dto.status, usageBeforeInsert = dto.usageBeforeInsert, usageBeforeUpdate = dto.usageBeforeUpdate, usageBeforeDelete = dto.usageBeforeDelete, usageAfterInsert = dto.usageAfterInsert, usageAfterUpdate = dto.usageAfterUpdate, usageAfterDelete = dto.usageAfterDelete, usageAfterUndelete = dto.usageAfterUndelete, lastModifiedDate = dto.lastModifiedDate, lastModifiedByName = dto.lastModifiedBy?.name, createdDate = dto.createdDate, createdByName = dto.createdBy?.name, numLinesCovered = dto.coverage?.numLinesCovered, numLinesUncovered = dto.coverage?.numLinesUncovered, body = dto.body))
        if (entity.body != null && entity.body != dto.body) {
            metadataHistoryRepository.save(MetadataHistory(sfdcId = dto.id, entityType = "ApexTrigger", body = entity.body))
        }
        triggerRepository.save(entity.copy(name = dto.name, sobject = dto.tableEnumOrId, apiVersion = dto.apiVersion, status = dto.status, usageBeforeInsert = dto.usageBeforeInsert, usageBeforeUpdate = dto.usageBeforeUpdate, usageBeforeDelete = dto.usageBeforeDelete, usageAfterInsert = dto.usageAfterInsert, usageAfterUpdate = dto.usageAfterUpdate, usageAfterDelete = dto.usageAfterDelete, usageAfterUndelete = dto.usageAfterUndelete, lastModifiedDate = dto.lastModifiedDate, lastModifiedByName = dto.lastModifiedBy?.name, createdDate = dto.createdDate, createdByName = dto.createdBy?.name, numLinesCovered = dto.coverage?.numLinesCovered, numLinesUncovered = dto.coverage?.numLinesUncovered, body = dto.body))
    }

    override fun syncReportsToDatabase(dtos: List<ReportDto>) = dtos.distinctBy { it.id }.forEach { dto ->
        val entity = reportRepository.findBySfdcId(dto.id).orElse(Report(sfdcId = dto.id, name = dto.name, developerName = dto.developerName, folderName = dto.folderName, createdDate = dto.createdDate, createdByName = dto.createdBy?.name, lastModifiedDate = dto.lastModifiedDate, lastModifiedByName = dto.lastModifiedBy?.name))
        reportRepository.save(entity.copy(name = dto.name, developerName = dto.developerName, folderName = dto.folderName, createdDate = dto.createdDate, createdByName = dto.createdBy?.name, lastModifiedDate = dto.lastModifiedDate, lastModifiedByName = dto.lastModifiedBy?.name))
    }

    private fun mapTriggerEvents(dto: ApexTriggerDto) = listOfNotNull(if (dto.usageBeforeInsert == true) "Before Insert" else null, if (dto.usageBeforeUpdate == true) "Before Update" else null, if (dto.usageBeforeDelete == true) "Before Delete" else null, if (dto.usageAfterInsert == true) "After Insert" else null, if (dto.usageAfterUpdate == true) "After Update" else null, if (dto.usageAfterDelete == true) "After Delete" else null, if (dto.usageAfterUndelete == true) "After Undelete" else null)
}
