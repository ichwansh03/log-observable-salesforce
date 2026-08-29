package com.observability.sfdc.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "logs", indexes = [
    Index(name = "idx_logs_apex_class_name", columnList = "apex_class_name"),
    Index(name = "idx_logs_author_name", columnList = "author_name"),
    Index(name = "idx_logs_request_time", columnList = "request_time"),
    Index(name = "idx_logs_sfdc_id", columnList = "sfdc_id")
])
data class Log(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "sfdc_id", unique = true)
    val sfdcId: String,

    @Column(name = "apex_class_name")
    val apexClassName: String? = null,

    @Column(name = "author_name")
    val authorName: String? = null,

    @Column(name = "request_time")
    val requestTime: Instant? = null,

    @Column(name = "operation")
    val operation: String? = null,

    @Column(name = "log_size")
    val logSize: Long? = null,

    @Column(name = "duration")
    val duration: Long? = null,

    @Column(name = "status")
    val status: String? = null,

    @Column(name = "request")
    val request: String? = null
)
