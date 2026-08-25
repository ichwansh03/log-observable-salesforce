package com.observability.sfdc.exception

import org.springframework.http.HttpStatus

class ResourceNotFoundException(
    message: String,
    val resource: String = "Resource",
    val resourceId: String? = null
) : AppException(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND")
