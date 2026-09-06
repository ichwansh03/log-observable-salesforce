package com.observability.sfdc.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value($$"${apexium.api-key:}") private val apiKey: String,
    @Value($$"${apexium.cors.allowed-origins:http://localhost:5173}") private val allowedOrigins: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val objectMapper = ObjectMapper()

        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors { }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                    .requestMatchers("/actuator/**").authenticated()
                if (apiKey.isNotBlank()) {
                    auth.requestMatchers("/api/sfdc/**").authenticated()
                } else {
                    auth.requestMatchers("/api/sfdc/**").permitAll()
                }
                auth.anyRequest().permitAll()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    val error = mapOf(
                        "status" to 401,
                        "error" to "Unauthorized",
                        "code" to "UNAUTHORIZED",
                        "message" to "Valid X-API-Key header required"
                    )
                    response.writer.write(objectMapper.writeValueAsString(error))
                }
            }

        if (apiKey.isNotBlank()) {
            http.addFilterBefore(ApiKeyAuthFilter(apiKey), UsernamePasswordAuthenticationFilter::class.java)
        }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = allowedOrigins.split(",").map { it.trim() }.toMutableList()
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
