package com.observability.sfdc.exception

import org.springframework.http.HttpStatus

class ConflictException(
    message: String,
    val resource: String = "Resource"
) : AppException(message, HttpStatus.CONFLICT, "CONFLICT")
