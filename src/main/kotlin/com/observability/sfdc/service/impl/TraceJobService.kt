package com.observability.sfdc.service.impl

import com.observability.sfdc.domain.TraceJob
import com.observability.sfdc.dto.FrontendTraceFlagRequest
import com.observability.sfdc.dto.TraceFlagDto
import com.observability.sfdc.repository.TraceJobRepository
import com.observability.sfdc.service.LogService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class TraceJobService(
    private val traceJobRepository: TraceJobRepository,
    private val logService: LogService
) {
    private val logger = LoggerFactory.getLogger(TraceJobService::class.java)
    private val sfdcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    private val sfdcWithOffsetFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    @Transactional
    fun createJob(request: FrontendTraceFlagRequest): TraceJob {
        val totalMinutes = request.getTotalMinutes()
        if (totalMinutes <= 0) {
            throw IllegalArgumentException("Total duration must be at least 1 minute")
        }

        val startTime = Instant.now()
        val endTime = startTime.plus(Duration.ofDays((request.durationDays ?: 0).toLong()))
            .plus(Duration.ofHours((request.durationHours ?: 0).toLong()))
            .plus(Duration.ofMinutes((request.durationMinutes ?: 0).toLong()))
        
        val job = TraceJob(
            tracedEntityId = request.tracedEntityId,
            tracedEntityName = request.tracedEntityName,
            tracedEntityType = request.entityType ?: "User",
            debugLevelName = request.debugLevelName,
            startTime = startTime,
            endTime = endTime,
            status = "ACTIVE"
        )
        
        val savedJob = traceJobRepository.save(job)
        
        // Trigger initial TraceFlag in Salesforce
        refreshSalesforceTraceFlag(savedJob)
        
        return savedJob
    }

    fun getAllJobs(): List<TraceJob> = traceJobRepository.findAll()

    fun searchJobsByName(name: String): List<TraceJob> = traceJobRepository.findByTracedEntityNameContainingIgnoreCase(name)

    @Transactional
    fun cancelJob(id: Long) {
        val job = traceJobRepository.findById(id).orElseThrow { RuntimeException("Job not found") }
        job.status = "CANCELLED"
        job.sfdcTraceFlagId?.let { 
            logService.deleteTraceFlag(it)
        }
        traceJobRepository.save(job)
    }

    @Transactional
    fun refreshSalesforceTraceFlag(job: TraceJob) {
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val jobEndTime = job.endTime.atZone(ZoneId.of("UTC"))
        
        val maxSafeExpiry = now.plusDays(1)
        val targetExpiry = if (jobEndTime.isBefore(maxSafeExpiry)) jobEndTime else maxSafeExpiry
        
        if (job.sfdcTraceFlagId == null) {
            // Create NEW
            val duration = Duration.between(now.toInstant(), targetExpiry.toInstant()).toMinutes().coerceAtLeast(1).toInt()
            val response = logService.createTraceFlag(
                FrontendTraceFlagRequest(
                    tracedEntityId = job.tracedEntityId,
                    debugLevelName = job.debugLevelName,
                    durationMinutes = duration,
                    entityType = job.tracedEntityType
                )
            )
            if (response?.success == true) {
                job.sfdcTraceFlagId = response.id
                traceJobRepository.save(job)
                logger.info("Job ${job.id}: Created initial Salesforce TraceFlag ${response.id} expiring at ${targetExpiry.format(sfdcFormatter)}")
            } else {
                logger.error("Job ${job.id}: Failed to create Salesforce TraceFlag: ${response?.errors}")
            }
        } else {
            // Sliding Window PATCH
            val success = logService.patchTraceFlag(
                job.sfdcTraceFlagId!!,
                now.format(sfdcFormatter),
                targetExpiry.format(sfdcFormatter)
            )
            if (success) {
                logger.info("Job ${job.id}: Slid window for TraceFlag ${job.sfdcTraceFlagId} to ${targetExpiry.format(sfdcFormatter)}")
            } else {
                logger.warn("Job ${job.id}: Failed to patch TraceFlag ${job.sfdcTraceFlagId}. Will attempt re-creation.")
                job.sfdcTraceFlagId = null
                traceJobRepository.save(job)
                // Recursive call to re-create
                refreshSalesforceTraceFlag(job)
            }
        }
    }

    @Transactional
    fun adoptExistingTraceFlag(traceFlag: TraceFlagDto): TraceJob {
        // Check if this SFDC TraceFlag is already managed by a local job
        val existingJob = traceJobRepository.findAll().find { it.sfdcTraceFlagId == traceFlag.id }
        if (existingJob != null) {
            throw IllegalStateException("This Salesforce TraceFlag is already managed by Job #${existingJob.id}")
        }

        val now = Instant.now()
        val startTime = if (traceFlag.startDate != null) {
            ZonedDateTime.parse(traceFlag.startDate, sfdcWithOffsetFormatter).toInstant()
        } else {
            now
        }
        val endTime = if (traceFlag.expirationDate != null) {
            ZonedDateTime.parse(traceFlag.expirationDate, sfdcWithOffsetFormatter).toInstant()
        } else {
            now.plus(Duration.ofHours(1))
        }

        val job = TraceJob(
            tracedEntityId = traceFlag.tracedEntityId,
            tracedEntityName = traceFlag.tracedEntity?.name,
            tracedEntityType = traceFlag.tracedEntity?.attributes?.type ?: "User",
            debugLevelName = traceFlag.debugLevel?.developerName ?: "Unknown",
            startTime = startTime,
            endTime = endTime,
            status = if (endTime.isAfter(now)) "ACTIVE" else "EXPIRED",
            sfdcTraceFlagId = traceFlag.id
        )
        
        val savedJob = traceJobRepository.save(job)
        logger.info("Adopted Salesforce TraceFlag ${traceFlag.id} as Job #${savedJob.id} (status: ${savedJob.status})")
        return savedJob
    }
}
