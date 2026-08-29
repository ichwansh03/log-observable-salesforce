package com.observability.sfdc.service

import com.observability.sfdc.dto.ApexLogDto
import java.io.InputStream

interface ApexLogService {
    fun queryApexLogs(limit: Int = 10, offset: Int = 0, fetchBody: Boolean = true): List<ApexLogDto>
    fun extractClassName(body: String?): String?
    fun getLogBody(logId: String): String?
    fun getLogDownloadStream(logId: String): InputStream?
    fun deleteLog(id: String): Boolean
    fun deleteLogs(ids: List<String>): Map<String, Boolean>
    fun deleteAllLogs(): Int
}
