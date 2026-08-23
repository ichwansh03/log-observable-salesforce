package com.observability.sfdc.service.impl

import com.observability.sfdc.dto.SalesforceTokenResponse
import com.observability.sfdc.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Service
class SalesforceAuthService(
    @Value($$"${salesforce.login-url}") private val loginUrl: String,
    @Value($$"${salesforce.client-id}") private val clientId: String,
    @Value($$"${salesforce.client-secret}") private val clientSecret: String,
    @Value($$"${salesforce.grant-type}") private val grantType: String
) : AuthService {
    private val restTemplate = RestTemplate()
    private val logger = LoggerFactory.getLogger(SalesforceAuthService::class.java)

    @Cacheable(value = ["sf_tokens"], key = "'client_credentials_token'", unless = "#result == null")
    override fun getAccessToken(): SalesforceTokenResponse? {
        val url = "$loginUrl/services/oauth2/token"
        
        logger.info("Attempting Salesforce authentication at: $url")
        logger.info("Using Grant Type: $grantType")
        
        if (clientId.isBlank()) {
            logger.error("SALESFORCE_CLIENT_ID is blank or missing!")
        } else {
            logger.info("Client ID successfully loaded")
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val map = LinkedMultiValueMap<String, String>()
        map.add("grant_type", grantType)
        map.add("client_id", clientId)
        map.add("client_secret", clientSecret)

        val request = HttpEntity(map, headers)

        return try {
            val response = restTemplate.postForObject(url, request, SalesforceTokenResponse::class.java)
            if (response?.accessToken == null || response.instanceUrl == null) {
                logger.error("Salesforce response missing essential fields: $response")
                return null
            }
            logger.info("Successfully authenticated with Salesforce.")
            response
        } catch (e: Exception) {
            logger.error("Error authenticating with Salesforce: ${e.message}")
            if (e is HttpClientErrorException) {
                logger.error("Response Body: ${e.responseBodyAsString}")
            }
            null
        }
    }
}
