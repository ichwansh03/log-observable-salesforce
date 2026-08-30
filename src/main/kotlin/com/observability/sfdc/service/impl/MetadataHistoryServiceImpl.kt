package com.observability.sfdc.service.impl

import com.observability.sfdc.dto.MetadataDiffDto
import com.observability.sfdc.dto.MetadataHistoryDto
import com.observability.sfdc.exception.ResourceNotFoundException
import com.observability.sfdc.repository.MetadataHistoryRepository
import com.observability.sfdc.service.MetadataHistoryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MetadataHistoryServiceImpl(
    private val historyRepository: MetadataHistoryRepository,
    private val minioService: MinioService
) : MetadataHistoryService {

    private val logger = LoggerFactory.getLogger(MetadataHistoryServiceImpl::class.java)

    override fun getHistory(entityType: String, sfdcId: String): List<MetadataHistoryDto> {
        validateEntityType(entityType)
        return historyRepository.findBySfdcIdAndEntityTypeOrderByCreatedAtDesc(sfdcId, entityType)
            .map { entity ->
                MetadataHistoryDto(
                    id = entity.id,
                    sfdcId = entity.sfdcId,
                    entityType = entity.entityType,
                    changedAt = entity.changedAt,
                    changedByName = entity.changedByName,
                    createdAt = entity.createdAt
                )
            }
    }

    override fun getDiff(entityType: String, sfdcId: String, historyId: Long): MetadataDiffDto? {
        validateEntityType(entityType)
        val latestBody = minioService.downloadMetadataBody(entityType, sfdcId)
            ?: throw ResourceNotFoundException("Current body not found in MinIO", "MetadataBody", sfdcId)

        val previousBody = minioService.downloadMetadataHistoryBody(entityType, sfdcId, historyId)
            ?: throw ResourceNotFoundException("Historical body not found in MinIO", "MetadataHistory", "$sfdcId/$historyId")

        return MetadataDiffDto(previousBody = previousBody, latestBody = latestBody)
    }

    override fun getDiffBetweenHistory(entityType: String, sfdcId: String, fromHistoryId: Long, toHistoryId: Long): MetadataDiffDto? {
        validateEntityType(entityType)
        val fromBody = minioService.downloadMetadataHistoryBody(entityType, sfdcId, fromHistoryId)
            ?: throw ResourceNotFoundException("Historical body not found in MinIO", "MetadataHistory", "$sfdcId/$fromHistoryId")

        val toBody = minioService.downloadMetadataHistoryBody(entityType, sfdcId, toHistoryId)
            ?: throw ResourceNotFoundException("Historical body not found in MinIO", "MetadataHistory", "$sfdcId/$toHistoryId")

        return MetadataDiffDto(previousBody = fromBody, latestBody = toBody)
    }

    private fun validateEntityType(type: String) {
        if (type != "ApexClass" && type != "ApexTrigger") {
            throw ResourceNotFoundException("Invalid metadata type: $type. Must be 'ApexClass' or 'ApexTrigger'", "EntityType", type)
        }
    }
}
