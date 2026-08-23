package com.observability.sfdc.service

import com.observability.sfdc.dto.ApexLogDto
import com.observability.sfdc.dto.FrontendTraceFlagRequest
import com.observability.sfdc.dto.SalesforceCreateResponse
import com.observability.sfdc.dto.TraceFlagDto
import java.io.InputStream

interface LogService {
    fun queryApexLogs(limit: Int = 10, offset: Int = 0, fetchBody: Boolean = true): List<ApexLogDto>
    fun extractClassName(body: String?): String?
    fun getLogBody(logId: String): String?
    fun getLogDownloadStream(logId: String): InputStream?
    fun createTraceFlag(frontendRequest: FrontendTraceFlagRequest): SalesforceCreateResponse?
    fun getActiveTraceFlags(): List<TraceFlagDto>
    fun getAllTraceFlags(): List<TraceFlagDto>
    fun deleteLog(id: String): Boolean
    fun deleteLogs(ids: List<String>): Map<String, Boolean>
    fun deleteAllLogs(): Int
    fun deleteTraceFlag(id: String): Boolean
    fun patchTraceFlag(id: String, startDate: String, expirationDate: String): Boolean
}
