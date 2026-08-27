package com.observability.sfdc.service

import com.observability.sfdc.domain.ApexClass
import com.observability.sfdc.domain.ApexTrigger
import com.observability.sfdc.domain.DebugLevel
import com.observability.sfdc.domain.Report
import com.observability.sfdc.dto.ApexClassDto
import com.observability.sfdc.dto.ApexTriggerDto
import com.observability.sfdc.dto.DebugLevelDto
import com.observability.sfdc.dto.MetadataDetailDto
import com.observability.sfdc.dto.ReportDescribeDto
import com.observability.sfdc.dto.ReportDto
import com.observability.sfdc.dto.ReportSoqlDto

interface MetadataService {
    fun getAllDebugLevels(name: String? = null, limit: Int = 10, offset: Int = 0): List<DebugLevelDto>
    fun getAllApexClasses(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexClassDto>
    fun getAllApexTriggers(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexTriggerDto>
    fun getAllReports(name: String? = null, limit: Int = 10, offset: Int = 0): List<ReportDto>
    fun getReportDescribe(reportId: String): ReportDescribeDto?
    fun convertReportToSoql(reportId: String): ReportSoqlDto?
    fun fetchApexClassesFromSalesforce(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexClassDto>
    fun fetchApexTriggersFromSalesforce(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexTriggerDto>
    fun searchClasses(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexClass>
    fun searchTriggers(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexTrigger>
    fun searchDebugLevels(name: String? = null, limit: Int = 10, offset: Int = 0): List<DebugLevel>
    fun searchReports(name: String? = null, limit: Int = 10, offset: Int = 0): List<Report>
    fun getMetadataDetail(id: String, type: String): MetadataDetailDto?
    fun syncDebugLevelsToDatabase(dtos: List<DebugLevelDto>)
    fun syncClassesToDatabase(dtos: List<ApexClassDto>)
    fun syncTriggersToDatabase(dtos: List<ApexTriggerDto>)
    fun syncReportsToDatabase(dtos: List<ReportDto>)
}
