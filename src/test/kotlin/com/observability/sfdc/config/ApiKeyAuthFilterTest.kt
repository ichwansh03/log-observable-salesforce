package com.observability.sfdc.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class ApiKeyAuthFilterTest {

    companion object {
        private const val VALID_KEY = "test-api-key-12345"
        private const val HEADER = "X-API-Key"
    }

    private lateinit var filter: ApiKeyAuthFilter

    @BeforeEach
    fun setup() {
        SecurityContextHolder.clearContext()
        filter = ApiKeyAuthFilter(VALID_KEY)
    }

    private fun doFilter(keyValue: String?): Boolean {
        val request = MockHttpServletRequest("GET", "/api/sfdc/logs")
        if (keyValue != null) {
            request.addHeader(HEADER, keyValue)
        }
        val response = MockHttpServletResponse()
        val chain = jakarta.servlet.FilterChain { _, _ -> /* no-op */ }
        filter.doFilter(request, response, chain)
        return SecurityContextHolder.getContext().authentication != null
    }

    @Test
    fun `accepts valid API key`() {
        assertTrue(doFilter(VALID_KEY))
        assertEquals("api-client", SecurityContextHolder.getContext().authentication?.name)
    }

    @Test
    fun `rejects invalid API key`() {
        assertFalse(doFilter("wrong-key"))
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `rejects missing API key header`() {
        assertFalse(doFilter(null))
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `rejects blank API key`() {
        assertFalse(doFilter(""))
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `rejects all requests when configured key is empty`() {
        val emptyKeyFilter = ApiKeyAuthFilter("")
        val request = MockHttpServletRequest("GET", "/api/sfdc/logs")
        request.addHeader(HEADER, "anything")
        val response = MockHttpServletResponse()
        val chain = jakarta.servlet.FilterChain { _, _ -> /* no-op */ }

        emptyKeyFilter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `sets ROLE_API authority`() {
        assertTrue(doFilter(VALID_KEY))
        val authorities = SecurityContextHolder.getContext().authentication?.authorities
        assertEquals(1, authorities?.size)
        assertEquals("ROLE_API", authorities?.first()?.authority)
    }
}
