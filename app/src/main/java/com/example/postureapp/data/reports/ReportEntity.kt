package com.example.postureapp.data.reports

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sessionId: String?,
    val frontImagePath: String?,
    val rightImagePath: String?,
    val frontLandmarksJson: String?,
    val rightLandmarksJson: String?,
    val frontMetricsJson: String?,
    val rightMetricsJson: String?,
    val pdfPath: String,
    val thumbnailPath: String?,
    val serverId: String? = null,
    val syncStatus: String = "PENDING",
    val version: Int = 1,
    val userId: String? = null,
    val lastSyncError: String? = null,
    val sessionClientId: String = id
)

