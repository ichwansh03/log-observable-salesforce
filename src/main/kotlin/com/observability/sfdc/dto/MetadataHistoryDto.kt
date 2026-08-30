package com.observability.sfdc.dto

import java.time.Instant

data class MetadataHistoryDto(
    val id: Long?,
    val sfdcId: String,
    val entityType: String,
    val changedAt: Instant?,
    val changedByName: String?,
    val createdAt: Instant
)
