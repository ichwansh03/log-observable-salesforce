package com.observability.sfdc.dto

data class ReportSoqlDto(
    val reportId: String,
    val reportName: String?,
    val rootObject: String?,
    val soql: String,
    val filters: List<ReportFilterDto>,
    val reportType: ReportTypeDto?,
    val reportFormat: String?,
    val objects: List<String>,
    val describeUrl: String?,
    val reportUrl: String?
)
