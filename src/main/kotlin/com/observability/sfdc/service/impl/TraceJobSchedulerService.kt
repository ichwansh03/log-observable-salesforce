package com.observability.sfdc.service.impl

import com.observability.sfdc.repository.TraceJobRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TraceJobSchedulerService(
    private val traceJobRepository: TraceJobRepository,
    private val traceJobService: TraceJobService
) {
    private val logger = LoggerFactory.getLogger(TraceJobSchedulerService::class.java)

    @Scheduled(fixedRate = 900000) // 15 minutes
    fun processJobs() {
        val activeJobs = traceJobRepository.findByStatus("ACTIVE")
        if (activeJobs.isEmpty()) return

        logger.info("Starting Trace Job maintenance cycle for ${activeJobs.size} jobs...")
        val now = Instant.now()

        activeJobs.forEach { job ->
            try {
                if (now.isAfter(job.endTime)) {
                    logger.info("Job ${job.id}: Target end time reached. Marking COMPLETED.")
                    job.status = "COMPLETED"
                    traceJobRepository.save(job)
                } else {
                    // Slide the window forward
                    traceJobService.refreshSalesforceTraceFlag(job)
                }
            } catch (e: Exception) {
                logger.error("Error processing Job ${job.id}: ${e.message}")
            }
        }
    }
}
