package com.observability.sfdc.service.impl

import com.observability.sfdc.domain.MetadataHistory
import com.observability.sfdc.dto.MetadataDiffDto
import com.observability.sfdc.exception.ResourceNotFoundException
import com.observability.sfdc.repository.MetadataHistoryRepository
import com.observability.sfdc.service.MetadataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MetadataComparisonService(
    private val metadataService: MetadataService,
    private val historyRepository: MetadataHistoryRepository,
    private val minioService: MinioService
) {

    @Transactional
    fun compareMetadata(entityId: String, type: String): MetadataDiffDto {
        val detail = metadataService.getMetadataDetail(entityId, type)
            ?: throw ResourceNotFoundException("Entity not found in Salesforce or has no body", "MetadataEntity", entityId)

        val latestBody = detail.body ?: throw ResourceNotFoundException("Entity has no body", "MetadataBody", entityId)

        val previousHistory = historyRepository.findTopBySfdcIdAndEntityTypeOrderByCreatedAtDesc(entityId, type)
        val previousBody = if (previousHistory != null) {
            minioService.downloadMetadataHistoryBody(type, entityId, previousHistory.id!!) ?: ""
        } else ""

        val history = historyRepository.save(MetadataHistory(sfdcId = entityId, entityType = type))
        minioService.uploadMetadataHistoryBody(type, entityId, history.id!!, latestBody)

        return MetadataDiffDto(previousBody = previousBody, latestBody = latestBody)
    }
}
