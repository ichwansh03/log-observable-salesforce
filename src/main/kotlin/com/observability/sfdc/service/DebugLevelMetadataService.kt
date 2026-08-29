package com.observability.sfdc.service

import com.observability.sfdc.domain.DebugLevel
import com.observability.sfdc.dto.DebugLevelDto

interface DebugLevelMetadataService {
    fun getAllDebugLevels(name: String? = null, limit: Int = 10, offset: Int = 0): List<DebugLevelDto>
    fun searchDebugLevels(name: String? = null, limit: Int = 10, offset: Int = 0): List<DebugLevel>
    fun syncDebugLevelsToDatabase(dtos: List<DebugLevelDto>)
}
