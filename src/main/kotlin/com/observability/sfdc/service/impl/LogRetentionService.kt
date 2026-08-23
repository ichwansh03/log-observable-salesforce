package com.observability.sfdc.service.impl

import com.observability.sfdc.repository.LogRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class LogRetentionService(
    private val logRepository: LogRepository,
    @Value($$"${log.retention.days:30}") private val retentionDays: Long
) {
    private val logger = LoggerFactory.getLogger(LogRetentionService::class.java)

    @Scheduled(cron = "\${log.retention.cron:0 0 3 * * ?}")
    @Transactional
    fun purgeOldLogs() {
        val cutoff = Instant.now().minusSeconds(retentionDays * 86400)
        logger.info("Starting log retention purge: removing logs older than $retentionDays days (before $cutoff)...")
        val deleted = logRepository.deleteByRequestTimeBefore(cutoff)
        logger.info("Log retention purge complete: $deleted logs deleted.")
    }
}
