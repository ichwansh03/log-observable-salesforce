package com.observability.sfdc.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val path: String? = null,
    val details: Map<String, Any>? = null
)
