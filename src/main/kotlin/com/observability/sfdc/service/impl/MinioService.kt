package com.observability.sfdc.service.impl

import io.minio.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Service
class MinioService(
    private val minioClient: MinioClient,
    @Value($$"${minio.bucket-name}") private val bucketName: String
) {
    private val logger = LoggerFactory.getLogger(MinioService::class.java)

    init {
        ensureBucketExists()
    }

    private fun ensureBucketExists() {
        try {
            val found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
                logger.info("Created MinIO bucket: $bucketName")
            }
        } catch (e: Exception) {
            logger.error("Error ensuring MinIO bucket exists: ${e.message}")
        }
    }

    fun exists(logId: String): Boolean {
        return try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`("$logId.log.gz")
                    .build()
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    @Async
    fun uploadLog(logId: String, body: String) {
        uploadLogSync(logId, body)
    }

    fun uploadLogSync(logId: String, body: String) {
        try {
            val compressedData = compress(body)
            val inputStream = ByteArrayInputStream(compressedData)
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`("$logId.log.gz")
                    .stream(inputStream, compressedData.size.toLong(), -1)
                    .contentType("application/gzip")
                    .build()
            )
            logger.info("Uploaded compressed log $logId to MinIO")
        } catch (e: Exception) {
            logger.error("Failed to upload log $logId to MinIO: ${e.message}")
        }
    }

    fun downloadLog(logId: String): String? {
        return try {
            val stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`("$logId.log.gz")
                    .build()
            )
            val compressedData = stream.use { it.readAllBytes() }
            decompress(compressedData)
        } catch (e: Exception) {
            // Log not found or other error - return null to fallback
            logger.error("Error downloading log $logId from MinIO: ${e.message}")
            null
        }
    }

    fun getDownloadStream(logId: String): InputStream? {
        return try {
            minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`("$logId.log.gz")
                    .build()
            )
        } catch (e: Exception) {
            logger.error("Failed to get stream for $logId: ${e.message}")
            null
        }
    }

    fun deleteLog(logId: String) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`("$logId.log.gz")
                    .build()
            )
            logger.info("Deleted log $logId from MinIO")
        } catch (e: Exception) {
            logger.error("Failed to delete log $logId from MinIO: ${e.message}")
        }
    }

    // --- Metadata body methods ---

    fun uploadMetadataBody(entityType: String, sfdcId: String, body: String) {
        try {
            val compressedData = compress(body)
            val inputStream = ByteArrayInputStream(compressedData)
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`("metadata/$entityType/$sfdcId/current.gz")
                    .stream(inputStream, compressedData.size.toLong(), -1)
                    .contentType("application/gzip")
                    .build()
            )
            logger.info("Uploaded metadata body $entityType/$sfdcId to MinIO")
        } catch (e: Exception) {
            logger.error("Failed to upload metadata body $entityType/$sfdcId to MinIO: ${e.message}")
        }
    }

    fun uploadMetadataHistoryBody(entityType: String, sfdcId: String, historyId: Long, body: String) {
        try {
            val compressedData = compress(body)
            val inputStream = ByteArrayInputStream(compressedData)
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`("metadata/$entityType/$sfdcId/$historyId.gz")
                    .stream(inputStream, compressedData.size.toLong(), -1)
                    .contentType("application/gzip")
                    .build()
            )
            logger.info("Uploaded metadata history body $entityType/$sfdcId/$historyId to MinIO")
        } catch (e: Exception) {
            logger.error("Failed to upload metadata history body $entityType/$sfdcId/$historyId to MinIO: ${e.message}")
        }
    }

    fun downloadMetadataBody(entityType: String, sfdcId: String): String? {
        return try {
            val stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`("metadata/$entityType/$sfdcId/current.gz")
                    .build()
            )
            val compressedData = stream.use { it.readAllBytes() }
            decompress(compressedData)
        } catch (e: Exception) {
            logger.debug("Metadata body $entityType/$sfdcId not found in MinIO: ${e.message}")
            null
        }
    }

    fun downloadMetadataHistoryBody(entityType: String, sfdcId: String, historyId: Long): String? {
        return try {
            val stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`("metadata/$entityType/$sfdcId/$historyId.gz")
                    .build()
            )
            val compressedData = stream.use { it.readAllBytes() }
            decompress(compressedData)
        } catch (e: Exception) {
            logger.debug("Metadata history body $entityType/$sfdcId/$historyId not found in MinIO: ${e.message}")
            null
        }
    }

    private fun compress(data: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data.toByteArray()) }
        return bos.toByteArray()
    }

    private fun decompress(compressedData: ByteArray): String {
        val bis = ByteArrayInputStream(compressedData)
        return GZIPInputStream(bis).bufferedReader().use { it.readText() }
    }
}
