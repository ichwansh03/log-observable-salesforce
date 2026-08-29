package com.observability.sfdc.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
class RedisConfig(
    @Value($$"${cache.ttl.token}") private val tokenTtl: Long,
    @Value($$"${cache.ttl.metadata}") private val metadataTtl: Long
) {

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val serializer = GenericJackson2JsonRedisSerializer()

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .entryTtl(Duration.ofSeconds(metadataTtl)) // Default TTL

        val cacheConfigs = mutableMapOf<String, RedisCacheConfiguration>()

        // Custom TTL for specific caches
        cacheConfigs["sf_tokens"] = defaultConfig.entryTtl(Duration.ofSeconds(tokenTtl))
        cacheConfigs["sf_metadata"] = defaultConfig.entryTtl(Duration.ofSeconds(metadataTtl))
        cacheConfigs["sf_users"] = defaultConfig.entryTtl(Duration.ofSeconds(metadataTtl))

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build()
    }
}
