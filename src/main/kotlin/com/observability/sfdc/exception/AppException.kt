package com.observability.sfdc.exception

import org.springframework.http.HttpStatus

open class AppException(
    override val message: String,
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    val code: String = "INTERNAL_ERROR"
) : RuntimeException(message)
