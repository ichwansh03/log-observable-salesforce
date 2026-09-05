package com.observability.sfdc.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

class ApiKeyAuthFilter(
    private val apiKey: String
) : OncePerRequestFilter() {

    companion object {
        const val HEADER_NAME = "X-API-Key"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val providedKey = request.getHeader(HEADER_NAME)

        if (!providedKey.isNullOrBlank() && isValidApiKey(providedKey)) {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_API"))
            val auth = UsernamePasswordAuthenticationToken("api-client", null, authorities)
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }

    private fun isValidApiKey(providedKey: String): Boolean {
        if (apiKey.isBlank()) return false
        return MessageDigest.isEqual(
            providedKey.toByteArray(Charsets.UTF_8),
            apiKey.toByteArray(Charsets.UTF_8)
        )
    }
}
