package com.observability.sfdc.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReportFilterDto(
    @JsonProperty("column")
    val column: String?,

    @JsonProperty("operator")
    val operator: String?,

    @JsonProperty("value")
    val value: String?
)
