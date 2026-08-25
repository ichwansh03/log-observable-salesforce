package com.observability.sfdc.exception

import org.springframework.http.HttpStatus

class ValidationException(
    message: String,
    val field: String? = null
) : AppException(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR")
