package com.observability.sfdc.repository

import com.observability.sfdc.domain.Report
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ReportRepository : JpaRepository<Report, Long> {
    fun findBySfdcId(sfdcId: String): Optional<Report>
    fun findByNameContainingIgnoreCaseOrDeveloperNameContainingIgnoreCase(name: String, developerName: String, pageable: Pageable): List<Report>
    fun findAllProjectedBy(pageable: Pageable): List<Report>
}
