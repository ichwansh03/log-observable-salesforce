package com.observability.sfdc.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReportDescribeDto(
    @JsonProperty("reportMetadata")
    val reportMetadata: ReportMetadataDto?
)
