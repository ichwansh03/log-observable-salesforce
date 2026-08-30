package com.observability.sfdc.service

import com.observability.sfdc.domain.ApexClass
import com.observability.sfdc.dto.ApexClassDto

interface ApexClassMetadataService {
    fun getAllApexClasses(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexClassDto>
    fun fetchApexClassesFromSalesforce(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexClassDto>
    fun countApexClassesFromSalesforce(): Int
    fun searchClasses(name: String? = null, limit: Int = 10, offset: Int = 0): List<ApexClass>
    fun syncClassesToDatabase(dtos: List<ApexClassDto>)
}
