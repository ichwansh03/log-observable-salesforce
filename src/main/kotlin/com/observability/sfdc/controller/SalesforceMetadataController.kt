package com.observability.sfdc.controller

import com.observability.sfdc.domain.ApexClass
import com.observability.sfdc.domain.ApexTrigger
import com.observability.sfdc.domain.Report
import com.observability.sfdc.dto.ApexClassDto
import com.observability.sfdc.dto.ApexTriggerDto
import com.observability.sfdc.dto.DebugLevelDto
import com.observability.sfdc.dto.MetadataDetailDto
import com.observability.sfdc.dto.MetadataDiffDto
import com.observability.sfdc.dto.MetadataHistoryDto
import com.observability.sfdc.dto.ReportDescribeDto
import com.observability.sfdc.dto.ReportDto
import com.observability.sfdc.dto.ReportSoqlDto
import com.observability.sfdc.service.ApexClassMetadataService
import com.observability.sfdc.service.ApexTriggerMetadataService
import com.observability.sfdc.service.DebugLevelMetadataService
import com.observability.sfdc.service.MetadataDetailService
import com.observability.sfdc.service.MetadataHistoryService
import com.observability.sfdc.service.ReportMetadataService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sfdc/metadata")
@Validated
@Tag(name = "Salesforce Metadata", description = "Endpoints for retrieving Salesforce metadata information (Classes, Triggers, Debug Levels)")
class SalesforceMetadataController(
    private val apexClassMetadataService: ApexClassMetadataService,
    private val apexTriggerMetadataService: ApexTriggerMetadataService,
    private val debugLevelMetadataService: DebugLevelMetadataService,
    private val reportMetadataService: ReportMetadataService,
    private val metadataDetailService: MetadataDetailService,
    private val metadataHistoryService: MetadataHistoryService
) {

    @GetMapping("/details/{type}/{id}")
    @Operation(summary = "Get Metadata Details", description = "Retrieves deep details for a specific Apex class or trigger, including coverage and related test classes.")
    fun getMetadataDetails(
        @PathVariable type: String,
        @PathVariable id: String
    ): MetadataDetailDto? {
        return metadataDetailService.getMetadataDetail(id, type)
    }

    @GetMapping("/history/{type}/{id}")
    @Operation(summary = "Get Metadata History", description = "Returns the change history timeline for a specific Apex class or trigger, ordered newest first.")
    fun getMetadataHistory(
        @PathVariable type: String,
        @PathVariable id: String
    ): List<MetadataHistoryDto> {
        return metadataHistoryService.getHistory(type, id)
    }

    @GetMapping("/history/{type}/{id}/diff")
    @Operation(summary = "Get Metadata Diff", description = "Returns a diff between the current body and a specific historical snapshot.")
    fun getMetadataDiff(
        @PathVariable type: String,
        @PathVariable id: String,
        @RequestParam historyId: Long
    ): MetadataDiffDto {
        return metadataHistoryService.getDiff(type, id, historyId)
            ?: throw com.observability.sfdc.exception.ResourceNotFoundException("Diff not available", "MetadataDiff", "$type/$id")
    }

    @GetMapping("/debug-levels")
    @Operation(summary = "Get Debug Levels from Salesforce", description = "Retrieves all available debug configurations directly from Salesforce.")
    fun getDebugLevels(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<DebugLevelDto> {
        val offset = page * size
        return debugLevelMetadataService.getAllDebugLevels(limit = size, offset = offset)
    }

    @GetMapping("/debug-levels/db")
    @Operation(summary = "Search Debug Levels in Database", description = "Searches for debug levels stored in the local database.")
    fun searchDebugLevels(
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<com.observability.sfdc.domain.DebugLevel> {
        val offset = page * size
        return debugLevelMetadataService.searchDebugLevels(name, size, offset)
    }

    @GetMapping("/classes")
    @Operation(summary = "Get Apex Classes from Salesforce", description = "Retrieves active Apex classes directly from Salesforce.")
    fun getApexClasses(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<ApexClassDto> {
        val offset = page * size
        return apexClassMetadataService.getAllApexClasses(limit = size, offset = offset)
    }

    @GetMapping("/classes/db")
    @Operation(summary = "Search Apex Classes in Database", description = "Searches for Apex classes stored in the local database.")
    fun searchClasses(
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<ApexClass> {
        val offset = page * size
        return apexClassMetadataService.searchClasses(name, size, offset)
    }

    @GetMapping("/triggers")
    @Operation(summary = "Get Apex Triggers from Salesforce", description = "Retrieves active Apex triggers directly from Salesforce.")
    fun getApexTriggers(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<ApexTriggerDto> {
        val offset = page * size
        return apexTriggerMetadataService.getAllApexTriggers(limit = size, offset = offset)
    }

    @GetMapping("/triggers/db")
    @Operation(summary = "Search Apex Triggers in Database", description = "Searches for Apex triggers stored in the local database.")
    fun searchTriggers(
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<ApexTrigger> {
        val offset = page * size
        return apexTriggerMetadataService.searchTriggers(name, size, offset)
    }

    @GetMapping("/reports")
    @Operation(summary = "Get Reports from Salesforce", description = "Retrieves report metadata directly from Salesforce.")
    fun getReports(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<ReportDto> {
        val offset = page * size
        return reportMetadataService.getAllReports(limit = size, offset = offset)
    }

    @GetMapping("/reports/db")
    @Operation(summary = "Search Reports in Database", description = "Searches for report metadata stored in the local database.")
    fun searchReports(
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<Report> {
        val offset = page * size
        return reportMetadataService.searchReports(name, size, offset)
    }

    @GetMapping("/reports/{id}/describe")
    @Operation(summary = "Get Report Describe", description = "Retrieves full report metadata including filters from Salesforce Analytics API.")
    fun getReportDescribe(
        @PathVariable id: String
    ): ReportDescribeDto? {
        return reportMetadataService.getReportDescribe(id)
    }

    @GetMapping("/reports/{id}/soql")
    @Operation(summary = "Convert Report to SOQL", description = "Converts report filters to a SOQL query string.")
    fun convertReportToSoql(
        @PathVariable id: String
    ): ReportSoqlDto? {
        return reportMetadataService.convertReportToSoql(id)
    }
}
