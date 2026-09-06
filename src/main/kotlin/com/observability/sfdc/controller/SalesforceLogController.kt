package com.observability.sfdc.controller

import com.observability.sfdc.domain.Log
import com.observability.sfdc.domain.TraceJob
import com.observability.sfdc.dto.ApexLogDto
import com.observability.sfdc.dto.FrontendTraceFlagRequest
import com.observability.sfdc.dto.SalesforceCreateResponse
import com.observability.sfdc.dto.TraceFlagDto
import com.observability.sfdc.repository.LogRepository
import com.observability.sfdc.service.ApexLogService
import com.observability.sfdc.service.TraceFlagService
import com.observability.sfdc.service.impl.TraceJobService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sfdc/logs")
@Validated
@Tag(name = "Salesforce Logs", description = "Endpoints for managing and retrieving Salesforce debug logs")
class SalesforceLogController(
    private val apexLogService: ApexLogService,
    private val traceFlagService: TraceFlagService,
    private val logRepository: LogRepository,
    private val traceJobService: TraceJobService
) {

    @GetMapping
    @Operation(summary = "Query Apex Logs from Salesforce", description = "Retrieves a list of Apex log headers directly from Salesforce Tooling API.")
    fun getApexLogs(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<ApexLogDto> {
        val offset = page * size
        return apexLogService.queryApexLogs(size, offset)
    }

    @GetMapping("/db")
    @Operation(summary = "Get Logs from Database", description = "Retrieves processed logs stored in the local PostgreSQL database with optional filtering.")
    fun getDbLogs(
        @RequestParam(required = false) className: String?,
        @RequestParam(required = false) author: String?,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<Log> {
        val pageable = PageRequest.of(page, size, Sort.by("requestTime").descending())
        return when {
            !className.isNullOrBlank() && !author.isNullOrBlank() ->
                logRepository.findByApexClassNameContainingIgnoreCaseAndAuthorNameContainingIgnoreCase(className, author, pageable)
            !className.isNullOrBlank() ->
                logRepository.findByApexClassNameContainingIgnoreCase(className, pageable)
            !author.isNullOrBlank() ->
                logRepository.findByAuthorNameContainingIgnoreCase(author, pageable)
            else -> logRepository.findAllByOrderByRequestTimeDesc(pageable)
        }
    }

    @GetMapping("/{id}/body")
    @Operation(summary = "Get Log Body", description = "Fetches the full text body of a specific Apex log, checking local storage first.")
    fun getLogBody(@PathVariable id: String): String {
        return apexLogService.getLogBody(id) ?: ""
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download Log File", description = "Downloads the compressed (.gz) log file from storage.")
    fun downloadLog(
        @PathVariable id: String,
        @RequestParam(required = false) operation: String?
    ): ResponseEntity<org.springframework.core.io.Resource> {
        val stream = apexLogService.getLogDownloadStream(id)
            ?: throw com.observability.sfdc.exception.ResourceNotFoundException("Log not available for download", "Log", id)
        val downloadName = "${operation ?: "log"}_$id.log.gz"
        val resource = org.springframework.core.io.InputStreamResource(stream)
        return ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.parseMediaType("application/gzip"))
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$downloadName\"")
            .body(resource)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Log", description = "Deletes a specific Apex log from Salesforce and local storage.")
    fun deleteLog(@PathVariable id: String): ResponseEntity<Unit> {
        apexLogService.deleteLog(id)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping
    @Operation(summary = "Bulk Delete Logs", description = "Deletes multiple logs by ID or deletes all logs if no IDs are provided.")
    fun deleteLogs(@RequestParam(required = false) ids: List<String>?): ResponseEntity<Map<String, Any>> {
        return if (ids.isNullOrEmpty()) {
            val count = apexLogService.deleteAllLogs()
            ResponseEntity.ok(mapOf("message" to "Successfully deleted $count logs from Salesforce", "count" to count))
        } else {
            val results = apexLogService.deleteLogs(ids)
            ResponseEntity.ok(mapOf("results" to results))
        }
    }

    @PostMapping("/trace-flags")
    @Operation(summary = "Create Trace Flag", description = "Creates a new TraceFlag in Salesforce for a target user or class.")
    fun createTraceFlag(@Valid @RequestBody request: FrontendTraceFlagRequest): SalesforceCreateResponse {
        return traceFlagService.createTraceFlag(request) ?: SalesforceCreateResponse(id = null, success = false, errors = listOf("Failed to create TraceFlag"))
    }

    @GetMapping("/trace-flags")
    @Operation(summary = "Get Active Trace Flags", description = "Lists all currently active TraceFlags in the Salesforce organization.")
    fun getActiveTraceFlags(): List<TraceFlagDto> {
        return traceFlagService.getActiveTraceFlags()
    }

    @GetMapping("/trace-flags/all")
    @Operation(summary = "Get All Trace Flags", description = "Lists all TraceFlags (active and expired) from the Salesforce organization.")
    fun getAllTraceFlags(): List<TraceFlagDto> {
        return traceFlagService.getAllTraceFlags()
    }

    @DeleteMapping("/trace-flags/{id}")
    @Operation(summary = "Delete Trace Flag", description = "Deletes a specific TraceFlag from Salesforce.")
    fun deleteTraceFlag(@PathVariable id: String): ResponseEntity<Unit> {
        traceFlagService.deleteTraceFlag(id)
        return ResponseEntity.ok().build()
    }

    // --- Trace Job Endpoints ---

    @PostMapping("/trace-jobs")
    @Operation(summary = "Create Trace Job", description = "Creates a managed trace job that automatically handles Salesforce's 24-hour limit using a sliding window.")
    fun createTraceJob(@Valid @RequestBody request: FrontendTraceFlagRequest): TraceJob {
        return traceJobService.createJob(request)
    }

    @GetMapping("/trace-jobs")
    @Operation(summary = "Get All Trace Jobs", description = "Lists all trace jobs (active, completed, cancelled) managed by the application.")
    fun getTraceJobs(@RequestParam(required = false) targetName: String?): List<TraceJob> {
        return if (!targetName.isNullOrBlank()) {
            traceJobService.searchJobsByName(targetName)
        } else {
            traceJobService.getAllJobs()
        }
    }

    @PostMapping("/trace-jobs/adopt")
    @Operation(summary = "Adopt Existing Trace Flag", description = "Imports an existing Salesforce TraceFlag as a managed trace job.")
    fun adoptTraceFlag(@Valid @RequestBody traceFlag: TraceFlagDto): TraceJob {
        return traceJobService.adoptExistingTraceFlag(traceFlag)
    }

    @DeleteMapping("/trace-jobs/{id}")
    @Operation(summary = "Cancel Trace Job", description = "Cancels a managed trace job and deletes its associated Salesforce TraceFlag.")
    fun cancelTraceJob(@PathVariable id: Long): ResponseEntity<Unit> {
        traceJobService.cancelJob(id)
        return ResponseEntity.ok().build()
    }
}
