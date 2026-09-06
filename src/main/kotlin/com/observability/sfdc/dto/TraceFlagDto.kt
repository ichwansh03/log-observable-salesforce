package com.observability.sfdc.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = true)
data class TraceFlagRequest(
    @JsonProperty("TracedEntityId")
    @field:NotBlank(message = "TracedEntityId is required")
    val tracedEntityId: String,
    
    @JsonProperty("DebugLevelId")
    @field:NotBlank(message = "DebugLevelId is required")
    val debugLevelId: String,
    
    @JsonProperty("LogType")
    @field:NotBlank(message = "LogType is required")
    val logType: String,
    
    @JsonProperty("StartDate")
    val startDate: String? = null,
    
    @JsonProperty("ExpirationDate")
    @field:NotBlank(message = "ExpirationDate is required")
    val expirationDate: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FrontendTraceFlagRequest(
    @field:NotBlank(message = "TracedEntityId is required")
    val tracedEntityId: String,

    val tracedEntityName: String? = null,

    @field:NotBlank(message = "DebugLevelName is required")
    val debugLevelName: String,
    @field:Min(value = 0, message = "Duration days must be non-negative")
    val durationDays: Int? = 0,

    @field:Min(value = 0, message = "Duration hours must be non-negative")
    val durationHours: Int? = 0,

    @field:Min(value = 0, message = "Duration minutes must be non-negative")
    val durationMinutes: Int? = 0,
    
    val entityType: String? = "User"
) {
    fun getTotalMinutes(): Long {
        return ((durationDays ?: 0).toLong() * 24 * 60) + ((durationHours ?: 0).toLong() * 60) + (durationMinutes ?: 0).toLong()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SalesforceCreateResponse(
    val id: String?,
    val success: Boolean,
    val errors: List<String> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TraceFlagDto(
    @JsonProperty("Id")
    @field:NotBlank(message = "TraceFlag Id is required")
    val id: String,
    @JsonProperty("TracedEntityId")
    @field:NotBlank(message = "TracedEntityId is required")
    val tracedEntityId: String,
    @JsonProperty("TracedEntity")
    val tracedEntity: TracedEntityDto?,
    @JsonProperty("StartDate")
    val startDate: String?,
    @JsonProperty("ExpirationDate")
    val expirationDate: String?,
    @JsonProperty("DebugLevelId")
    val debugLevelId: String?,
    @JsonProperty("DebugLevel")
    val debugLevel: DebugLevelSummaryDto?,
    @JsonProperty("LogType")
    val logType: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TracedEntityDto(
    @JsonProperty("Name")
    val name: String?,
    @JsonProperty("attributes")
    val attributes: EntityAttributesDto?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EntityAttributesDto(
    @JsonProperty("type")
    val type: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DebugLevelSummaryDto(
    @JsonProperty("DeveloperName")
    val developerName: String?
)
