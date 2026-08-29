package com.observability.sfdc.service

import com.observability.sfdc.dto.FrontendTraceFlagRequest
import com.observability.sfdc.dto.SalesforceCreateResponse
import com.observability.sfdc.dto.TraceFlagDto

interface TraceFlagService {
    fun createTraceFlag(frontendRequest: FrontendTraceFlagRequest): SalesforceCreateResponse?
    fun getActiveTraceFlags(): List<TraceFlagDto>
    fun getAllTraceFlags(): List<TraceFlagDto>
    fun deleteTraceFlag(id: String): Boolean
    fun patchTraceFlag(id: String, startDate: String, expirationDate: String): Boolean
}
