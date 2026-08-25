package com.observability.sfdc.exception

import org.springframework.http.HttpStatus

class AuthenticationException(
    message: String = "Salesforce authentication failed",
    cause: Throwable? = null
) : AppException(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR")
