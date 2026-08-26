package com.observability.sfdc.domain

import jakarta.persistence.*

@Entity
@Table(name = "reports")
data class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "sfdc_id", unique = true)
    val sfdcId: String,

    val name: String?,
    val description: String?,
    val developerName: String?,
    val folderName: String?,
    val format: String?,
    val reportType: String?,
    val createdDate: String?,
    val createdByName: String?,
    val lastModifiedDate: String?,
    val lastModifiedByName: String?
)
