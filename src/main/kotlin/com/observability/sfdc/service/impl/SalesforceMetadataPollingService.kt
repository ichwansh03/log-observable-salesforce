package com.observability.sfdc.service.impl

import com.observability.sfdc.service.ApexClassMetadataService
import com.observability.sfdc.service.ApexTriggerMetadataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SalesforceMetadataPollingService(
    private val apexClassMetadataService: ApexClassMetadataService,
    private val apexTriggerMetadataService: ApexTriggerMetadataService,
    @Value($$"${metadata.poll.batch-size:200}") private val batchSize: Int
) {
    private val logger = LoggerFactory.getLogger(SalesforceMetadataPollingService::class.java)

    @Scheduled(fixedRateString = $$"${metadata.poll.rate}")
    @Transactional
    fun pollMetadata() {
        logger.info("Starting Salesforce metadata polling cycle...")
        try {
            pollApexClasses()
            pollApexTriggers()
            logger.info("Metadata polling complete.")
        } catch (e: Exception) {
            logger.error("Critical error during Salesforce metadata polling: ${e.message}", e)
        }
    }

    private fun pollApexClasses() {
        val totalCount = apexClassMetadataService.countApexClassesFromSalesforce()
        logger.info("ApexClass total count from Salesforce: $totalCount")

        var offset = 0
        var fetchedCount = 0

        while (offset < totalCount) {
            val batch = apexClassMetadataService.fetchApexClassesFromSalesforce(limit = batchSize, offset = offset)
            if (batch.isEmpty()) break

            apexClassMetadataService.syncClassesToDatabase(batch)
            fetchedCount += batch.size
            offset += batchSize
            logger.info("ApexClass poll progress: $fetchedCount/$totalCount fetched")
        }

        if (fetchedCount != totalCount) {
            logger.warn("ApexClass count mismatch: expected $totalCount, fetched $fetchedCount (records may have been created/deleted during poll)")
        } else {
            logger.info("Synchronized $fetchedCount Apex classes.")
        }
    }

    private fun pollApexTriggers() {
        val totalCount = apexTriggerMetadataService.countApexTriggersFromSalesforce()
        logger.info("ApexTrigger total count from Salesforce: $totalCount")

        var offset = 0
        var fetchedCount = 0

        while (offset < totalCount) {
            val batch = apexTriggerMetadataService.fetchApexTriggersFromSalesforce(limit = batchSize, offset = offset)
            if (batch.isEmpty()) break

            apexTriggerMetadataService.syncTriggersToDatabase(batch)
            fetchedCount += batch.size
            offset += batchSize
            logger.info("ApexTrigger poll progress: $fetchedCount/$totalCount fetched")
        }

        if (fetchedCount != totalCount) {
            logger.warn("ApexTrigger count mismatch: expected $totalCount, fetched $fetchedCount (records may have been created/deleted during poll)")
        } else {
            logger.info("Synchronized $fetchedCount Apex triggers.")
        }
    }
}
