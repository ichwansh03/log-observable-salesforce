package com.observability.sfdc.service

import com.observability.sfdc.domain.Report
import com.observability.sfdc.dto.ReportDescribeDto
import com.observability.sfdc.dto.ReportDto
import com.observability.sfdc.dto.ReportSoqlDto

interface ReportMetadataService {
    fun getAllReports(name: String? = null, limit: Int = 10, offset: Int = 0): List<ReportDto>
    fun searchReports(name: String? = null, limit: Int = 10, offset: Int = 0): List<Report>
    fun syncReportsToDatabase(dtos: List<ReportDto>)
    fun getReportDescribe(reportId: String): ReportDescribeDto?
    fun convertReportToSoql(reportId: String): ReportSoqlDto?
}
