package com.observability.sfdc.exception

import org.springframework.http.HttpStatus

class MinioServiceException(
    message: String,
    val operation: String? = null,
    cause: Throwable? = null
) : AppException(message, HttpStatus.INTERNAL_SERVER_ERROR, "MINIO_ERROR")
