package com.observability.sfdc.service

import com.observability.sfdc.dto.ApexClassDto
import com.observability.sfdc.dto.ApexCodeCoverageDto
import com.observability.sfdc.dto.ApexTriggerDto
import com.observability.sfdc.repository.ApexClassRepository
import com.observability.sfdc.repository.ApexTriggerRepository
import com.observability.sfdc.repository.DebugLevelRepository
import com.observability.sfdc.service.impl.SalesforceAuthService
import com.observability.sfdc.service.impl.SalesforceMetadataService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.*
import java.util.*

class SalesforceMetadataServiceTest {

    private lateinit var authService: SalesforceAuthService
    private lateinit var classRepository: ApexClassRepository
    private lateinit var triggerRepository: ApexTriggerRepository
    private lateinit var debugLevelRepository: DebugLevelRepository
    private lateinit var service: SalesforceMetadataService

    // Helpers to avoid NPE with Kotlin non-nullable parameters
    private fun anyString(): String = any() ?: ""
    private fun <T> anyRef(): T = any() ?: null as T

    @BeforeEach
    fun setUp() {
        authService = mock(SalesforceAuthService::class.java)
        classRepository = mock(ApexClassRepository::class.java)
        triggerRepository = mock(ApexTriggerRepository::class.java)
        debugLevelRepository = mock(DebugLevelRepository::class.java)
        //service = spy(SalesforceMetadataService(authService, classRepository, triggerRepository, debugLevelRepository, "v60.0"))
    }

    @Test
    fun `getAllApexClasses should enrich with coverage data`() {
        // Arrange
        val classId = "01p000000000001"
        val classDto = ApexClassDto(id = classId, name = "MyClass", apiVersion = 60.0, status = "Active", lengthWithoutComments = 100, lastModifiedDate = null, lastModifiedBy = null, createdDate = null, createdBy = null)
        val coverageDto = ApexCodeCoverageDto(apexClassOrTriggerId = classId, numLinesCovered = 80, numLinesUncovered = 20)
        
        // Mock querySalesforce for ApexClass then coverage
        doReturn(listOf(classDto))
            .doReturn(listOf(coverageDto))
            .`when`(service).querySalesforce<Any>(
                anyString(),
                anyString(),
                anyRef(),
                anyBoolean()
            )
        
        `when`(classRepository.findBySfdcId(anyString())).thenReturn(Optional.empty())

        // Act
        val result = service.getAllApexClasses()

        // Assert
        assertEquals(1, result.size)
        val enrichedClass = result[0]
        assertNotNull(enrichedClass.coverage)
        assertEquals(80, enrichedClass.coverage?.numLinesCovered)
        assertEquals(80.0, enrichedClass.coverage?.coveragePercentage)
        
        verify(classRepository, times(1)).save(any())
    }

    @Test
    fun `getAllApexTriggers should enrich with coverage data`() {
        // Arrange
        val triggerId = "01q000000000001"
        val triggerDto = ApexTriggerDto(id = triggerId, name = "MyTrigger", tableEnumOrId = "Account", apiVersion = 60.0, status = "Active", lastModifiedDate = null, lastModifiedBy = null, createdDate = null, createdBy = null, usageBeforeInsert = true, usageBeforeUpdate = null, usageBeforeDelete = null, usageAfterInsert = null, usageAfterUpdate = null, usageAfterDelete = null, usageAfterUndelete = null)
        val coverageDto = ApexCodeCoverageDto(apexClassOrTriggerId = triggerId, numLinesCovered = 50, numLinesUncovered = 50)
        
        // Mock querySalesforce for ApexTrigger then coverage
        doReturn(listOf(triggerDto))
            .doReturn(listOf(coverageDto))
            .`when`(service).querySalesforce<Any>(
                anyString(),
                anyString(),
                anyRef(),
                anyBoolean()
            )
        
        `when`(triggerRepository.findBySfdcId(anyString())).thenReturn(Optional.empty())

        // Act
        val result = service.getAllApexTriggers()

        // Assert
        assertEquals(1, result.size)
        val enrichedTrigger = result[0]
        assertNotNull(enrichedTrigger.coverage)
        assertEquals(50, enrichedTrigger.coverage?.numLinesCovered)
        assertEquals(50.0, enrichedTrigger.coverage?.coveragePercentage)
        
        verify(triggerRepository, times(1)).save(any())
    }
}
