package com.observability.sfdc.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReportDto(
    @JsonProperty("Id")
    @field:NotBlank(message = "Salesforce ID is required")
    val id: String,

    @JsonProperty("Name")
    val name: String?,

    @JsonProperty("Description")
    val description: String?,

    @JsonProperty("DeveloperName")
    val developerName: String?,

    @JsonProperty("FolderName")
    val folderName: String?,

    @JsonProperty("Format")
    val format: String?,

    @JsonProperty("Type")
    val reportType: String?,

    @JsonProperty("CreatedDate")
    val createdDate: String?,

    @JsonProperty("CreatedBy")
    val createdBy: UserSummaryDto?,

    @JsonProperty("LastModifiedDate")
    val lastModifiedDate: String?,

    @JsonProperty("LastModifiedBy")
    val lastModifiedBy: UserSummaryDto?
)
