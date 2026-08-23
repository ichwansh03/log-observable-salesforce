package com.observability.sfdc.service.impl

import com.observability.sfdc.domain.MetadataHistory
import com.observability.sfdc.dto.MetadataDiffDto
import com.observability.sfdc.repository.MetadataHistoryRepository
import com.observability.sfdc.service.MetadataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MetadataComparisonService(
    private val metadataService: MetadataService,
    private val historyRepository: MetadataHistoryRepository
) {

    @Transactional
    fun compareMetadata(entityId: String, type: String): MetadataDiffDto {
        val detail = metadataService.getMetadataDetail(entityId, type)
            ?: throw IllegalArgumentException("Entity not found in Salesforce or has no body")

        val latestBody = detail.body ?: throw IllegalArgumentException("Entity has no body")

        val previousHistory = historyRepository.findTopBySfdcIdAndEntityTypeOrderByCreatedAtDesc(entityId, type)
        val previousBody = previousHistory?.body ?: ""

        historyRepository.save(MetadataHistory(sfdcId = entityId, entityType = type, body = latestBody))

        return MetadataDiffDto(previousBody = previousBody, latestBody = latestBody)
    }

    @Transactional
    fun saveHistory(entityId: String, type: String, body: String) {
        historyRepository.save(MetadataHistory(sfdcId = entityId, entityType = type, body = body))
    }
}
