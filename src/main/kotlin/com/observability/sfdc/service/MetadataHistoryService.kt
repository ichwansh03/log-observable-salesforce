package com.observability.sfdc.service

import com.observability.sfdc.dto.MetadataDiffDto
import com.observability.sfdc.dto.MetadataHistoryDto

interface MetadataHistoryService {
    fun getHistory(entityType: String, sfdcId: String): List<MetadataHistoryDto>
    fun getDiff(entityType: String, sfdcId: String, historyId: Long): MetadataDiffDto?
    fun getDiffBetweenHistory(entityType: String, sfdcId: String, fromHistoryId: Long, toHistoryId: Long): MetadataDiffDto?
}
