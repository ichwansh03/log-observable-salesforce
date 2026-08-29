package com.observability.sfdc.service.impl

import com.observability.sfdc.repository.ApexClassRepository
import com.observability.sfdc.repository.ApexTriggerRepository
import com.observability.sfdc.service.ApexClassMetadataService
import com.observability.sfdc.service.ApexTriggerMetadataService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SalesforceMetadataPollingService(
    private val apexClassMetadataService: ApexClassMetadataService,
    private val apexTriggerMetadataService: ApexTriggerMetadataService,
    private val classRepository: ApexClassRepository,
    private val triggerRepository: ApexTriggerRepository
) {
    private val logger = LoggerFactory.getLogger(SalesforceMetadataPollingService::class.java)

    @Scheduled(fixedRate = 3600000) // Poll every hour
    @Transactional
    fun pollMetadata() {
        logger.info("Starting Salesforce metadata polling cycle...")
        try {
            val classes = apexClassMetadataService.fetchApexClassesFromSalesforce(limit = 200)
            apexClassMetadataService.syncClassesToDatabase(classes)
            logger.info("Synchronized ${classes.size} Apex classes.")

            val triggers = apexTriggerMetadataService.fetchApexTriggersFromSalesforce(limit = 200)
            apexTriggerMetadataService.syncTriggersToDatabase(triggers)
            logger.info("Synchronized ${triggers.size} Apex triggers.")
            
            logger.info("Metadata polling complete.")
        } catch (e: Exception) {
            logger.error("Critical error during Salesforce metadata polling: ${e.message}", e)
        }
    }
}
