package com.example.postureapp.data.reports

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "report_assets")
data class ReportAssetEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val side: String,          // FRONT | RIGHT
    val kind: String,          // ORIGINAL | CROPPED
    val localPath: String,
    val sha256: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val serverId: String? = null,
    val syncStatus: String = "PENDING",
    val createdAt: Long,
    val updatedAt: Long
)

