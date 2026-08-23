package com.observability.sfdc.service

import com.observability.sfdc.dto.SalesforceQueryResult
import com.observability.sfdc.service.base.SalesforceServiceInterface
import com.observability.sfdc.service.impl.SalesforceAuthService
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

abstract class SalesforceBaseService(
    protected val authService: SalesforceAuthService,
    protected val apiVersion: String
) : SalesforceServiceInterface {
    protected val logger = LoggerFactory.getLogger(this::class.java)
    protected val restTemplate = RestTemplate()

    internal open fun <T> executeWithToken(
        operationName: String,
        fallback: T,
        action: (token: String, instanceUrl: String) -> T
    ): T {
        val tokenResponse = authService.getAccessToken() ?: return fallback
        return try {
            action(tokenResponse.accessToken!!, tokenResponse.instanceUrl!!)
        } catch (e: Exception) {
            logger.error("Error $operationName: ${e.message}")
            fallback
        }
    }

    protected fun createHeaders(token: String, contentType: MediaType? = null): HttpHeaders {
        return HttpHeaders().apply {
            setBearerAuth(token)
            contentType?.let { this.contentType = it }
        }
    }

    protected fun buildUri(instanceUrl: String, path: String, useTooling: Boolean = true): UriComponentsBuilder {
        val apiPath = if (useTooling) "tooling" else ""
        val baseUrl = if (apiPath.isNotEmpty()) "$instanceUrl/services/data/$apiVersion/$apiPath/$path"
                      else "$instanceUrl/services/data/$apiVersion/$path"
        return UriComponentsBuilder.fromUriString(baseUrl)
    }

    override fun <T> querySalesforce(
        operationName: String,
        query: String,
        typeReference: ParameterizedTypeReference<SalesforceQueryResult<T>>,
        useTooling: Boolean
    ): List<T> {
        return executeWithToken(operationName, emptyList()) { token, instanceUrl ->
            val uri = buildUri(instanceUrl, "query", useTooling)
                .queryParam("q", query)
                .build()
                .toUri()

            val entity = HttpEntity<Unit>(createHeaders(token))
            val response = restTemplate.exchange(uri, HttpMethod.GET, entity, typeReference)
            val result = response.body
            val records = result?.records ?: emptyList()
            
            if (records.isEmpty()) {
                logger.info("$operationName: Query returned 0 records. Total size in Salesforce: ${result?.totalSize ?: "unknown"}")
            }
            
            records
        }
    }
}
