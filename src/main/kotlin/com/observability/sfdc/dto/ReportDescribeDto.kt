package com.observability.sfdc.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReportDescribeDto(
    @JsonProperty("reportMetadata")
    val reportMetadata: ReportMetadataDto?,

    @JsonProperty("reportTypeMetadata")
    val reportTypeMetadata: ReportTypeMetadataDto?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReportTypeMetadataDto(
    @JsonProperty("categories")
    val categories: List<ReportCategoryDto>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReportCategoryDto(
    @JsonProperty("name")
    val name: String?,

    @JsonProperty("label")
    val label: String?
)
