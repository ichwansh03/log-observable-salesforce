package com.observability.sfdc.service.impl

import com.observability.sfdc.domain.Log
import com.observability.sfdc.repository.LogRepository
import com.observability.sfdc.service.ApexLogService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
class SalesforceLogPollingService(
    private val apexLogService: ApexLogService,
    private val logRepository: LogRepository
) {
    private val logger = LoggerFactory.getLogger(SalesforceLogPollingService::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    @Scheduled(fixedRate = 60000)
    @Transactional
    fun pollLogs() {
        logger.info("Starting Salesforce log polling cycle...")
        try {
            // Fetch logs without bodies first to check against database
            val logs = apexLogService.queryApexLogs(limit = 20, fetchBody = false)
            logger.info("Retrieved ${logs.size} log headers from Salesforce.")
            
            var newLogsCount = 0
            logs.forEach { dto ->
                if (!logRepository.findBySfdcId(dto.id).isPresent) {
                    // Fetch body from MinIO/SF, extract className, store body in MinIO only
                    val body = apexLogService.getLogBody(dto.id)
                    val apexClassName = dto.apexClassName ?: apexLogService.extractClassName(body)

                    val log = Log(
                        sfdcId = dto.id,
                        apexClassName = apexClassName,
                        authorName = dto.logUser?.name,
                        requestTime = parseDateTime(dto.startTime),
                        operation = dto.operation,
                        logSize = dto.logLength,
                        duration = dto.durationMilliseconds,
                        status = dto.status,
                        request = dto.request
                    )
                    logRepository.save(log)
                    newLogsCount++
                }
            }
            
            if (newLogsCount > 0) {
                logger.info("Success: Saved $newLogsCount new logs to PostgreSQL and MinIO.")
            } else {
                logger.info("Poll complete: No new logs found to save.")
            }
        } catch (e: Exception) {
            logger.error("Critical error during Salesforce log polling: ${e.message}", e)
        }
    }

    private fun parseDateTime(startTime: String?): Instant? {
        if (startTime == null) return null
        return try {
            // Salesforce format: 2026-05-09T10:00:00.000+0000
            OffsetDateTime.parse(startTime, dateTimeFormatter).toInstant()
        } catch (e: Exception) {
            logger.warn("Failed to parse start time: $startTime. Error: ${e.message}")
            null
        }
    }
}
