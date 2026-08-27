package com.observability.sfdc.dto

data class ReportSoqlDto(
    val reportId: String,
    val reportName: String?,
    val rootObject: String?,
    val soql: String,
    val filters: List<ReportFilterDto>
)
