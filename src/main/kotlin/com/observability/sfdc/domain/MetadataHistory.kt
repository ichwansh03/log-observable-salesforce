package com.observability.sfdc.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "metadata_history")
data class MetadataHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "sfdc_id", nullable = false)
    val sfdcId: String,

    @Column(name = "entity_type", nullable = false)
    val entityType: String, // ApexClass or ApexTrigger

    @Column(name = "changed_at")
    val changedAt: Instant? = null,

    @Column(name = "changed_by_name")
    val changedByName: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
