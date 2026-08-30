package com.observability.sfdc.service

import com.observability.sfdc.domain.ApexTrigger
import com.observability.sfdc.dto.ApexTriggerDto

interface ApexTriggerMetadataService {
    fun getAllApexTriggers(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexTriggerDto>
    fun fetchApexTriggersFromSalesforce(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexTriggerDto>
    fun countApexTriggersFromSalesforce(): Int
    fun searchTriggers(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexTrigger>
    fun syncTriggersToDatabase(dtos: List<ApexTriggerDto>)
}
