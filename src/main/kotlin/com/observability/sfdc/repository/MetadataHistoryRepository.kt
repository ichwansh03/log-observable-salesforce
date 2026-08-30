package com.observability.sfdc.repository

import com.observability.sfdc.domain.MetadataHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MetadataHistoryRepository : JpaRepository<MetadataHistory, Long> {
    fun findBySfdcIdAndEntityTypeOrderByCreatedAtDesc(sfdcId: String, entityType: String): List<MetadataHistory>
}
