package com.observability.sfdc.service

import com.observability.sfdc.dto.SalesforceTokenResponse

interface AuthService {
    fun getAccessToken(): SalesforceTokenResponse?
}
