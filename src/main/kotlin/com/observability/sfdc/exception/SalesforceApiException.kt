package com.observability.sfdc.exception

import org.springframework.http.HttpStatus

class SalesforceApiException(
    message: String,
    val operation: String? = null,
    cause: Throwable? = null
) : AppException(message, HttpStatus.BAD_GATEWAY, "SALESFORCE_API_ERROR")
