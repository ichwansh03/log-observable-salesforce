package com.observability.sfdc.service.base

import com.observability.sfdc.dto.SalesforceQueryResult
import org.springframework.core.ParameterizedTypeReference

interface SalesforceServiceInterface {
    fun <T> querySalesforce(
        operationName: String,
        query: String,
        typeReference: ParameterizedTypeReference<SalesforceQueryResult<T>>,
        useTooling: Boolean = true
    ): List<T>
}
