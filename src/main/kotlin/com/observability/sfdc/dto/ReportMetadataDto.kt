package com.observability.sfdc.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReportMetadataDto(
    @JsonProperty("id")
    val id: String?,

    @JsonProperty("name")
    val name: String?,

    @JsonProperty("reportFormat")
    val reportFormat: String?,

    @JsonProperty("reportType")
    val reportType: ReportTypeDto?,

    @JsonProperty("detailColumns")
    val detailColumns: List<String>?,

    @JsonProperty("reportFilters")
    val reportFilters: List<ReportFilterDto>?,

    @JsonProperty("reportBooleanFilter")
    val reportBooleanFilter: String?,

    @JsonProperty("developerName")
    val developerName: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReportTypeDto(
    @JsonProperty("type")
    val type: String?,

    @JsonProperty("label")
    val label: String?
)
