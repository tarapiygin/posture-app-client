package com.example.postureapp.data.reports.sync

import com.example.postureapp.data.reports.ReportAssetEntity
import com.example.postureapp.data.reports.ReportEntity
import com.example.postureapp.data.reports.sync.dto.FrontMetricsDto
import com.example.postureapp.data.reports.sync.dto.LandmarkSetDto
import com.example.postureapp.data.reports.sync.dto.RemoteReportItem
import com.example.postureapp.data.reports.sync.dto.ReportAssetRefDto
import com.example.postureapp.data.reports.sync.dto.ReportAssetsPayload
import com.example.postureapp.data.reports.sync.dto.ReportUploadDto
import com.example.postureapp.data.reports.sync.dto.RightMetricsDto
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

object ReportSyncMappers {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun ReportEntity.toUploadDto(assets: List<ReportAssetEntity> = emptyList()): ReportUploadDto {
        val frontLandmarks = frontLandmarksJson?.let { decodeOrNull<LandmarkSetDto>(it) }
        val rightLandmarks = rightLandmarksJson?.let { decodeOrNull<LandmarkSetDto>(it) }
        val frontMetrics = frontMetricsJson?.let { decodeOrNull<FrontMetricsDto>(it) }
        val rightMetrics = rightMetricsJson?.let { decodeOrNull<RightMetricsDto>(it) }
        val frontAssets = mutableListOf<ReportAssetRefDto>()
        val rightAssets = mutableListOf<ReportAssetRefDto>()
        assets.forEach { asset ->
            val ref = asset.serverId?.let {
                ReportAssetRefDto(
                    serverId = it,
                    side = asset.side,
                    kind = asset.kind
                )
            } ?: return@forEach
            when (asset.side.uppercase()) {
                "FRONT" -> frontAssets.add(ref)
                "RIGHT" -> rightAssets.add(ref)
            }
        }

        return ReportUploadDto(
            clientId = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            sessionId = sessionId,
            sessionClientId = sessionClientId,
            userId = userId,
            frontImagePath = frontImagePath,
            rightImagePath = rightImagePath,
            frontLandmarks = frontLandmarks,
            rightLandmarks = rightLandmarks,
            frontMetrics = frontMetrics,
            rightMetrics = rightMetrics,
            assets = ReportAssetsPayload(
                front = frontAssets,
                right = rightAssets
            ),
            version = version
        )
    }

    fun File.toPdfPart(): MultipartBody.Part =
        MultipartBody.Part.createFormData(
            name = "pdf",
            filename = this.name,
            body = this.asRequestBody("application/pdf".toMediaType())
        )

    fun ReportUploadDto.toRequestBody(): RequestBody =
        json.encodeToString(ReportUploadDto.serializer(), this)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

    fun File.toImagePart(name: String = "file"): MultipartBody.Part =
        MultipartBody.Part.createFormData(
            name = name,
            filename = this.name,
            body = this.asRequestBody(guessImageMimeType().toMediaType())
        )

    fun <T> T.asJsonPart(serializer: kotlinx.serialization.KSerializer<T>): RequestBody =
        json.encodeToString(serializer, this)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

    fun RemoteReportItem.toReportEntity(pdfPath: String, thumbnailPath: String? = null): ReportEntity {
        val metadata = metadata
        return ReportEntity(
            id = metadata.clientId,
            createdAt = metadata.createdAt,
            updatedAt = metadata.updatedAt,
            sessionId = metadata.sessionId,
            sessionClientId = metadata.sessionClientId,
            frontImagePath = metadata.frontImagePath,
            rightImagePath = metadata.rightImagePath,
            frontLandmarksJson = metadata.frontLandmarks?.let { json.encodeToString(it) },
            rightLandmarksJson = metadata.rightLandmarks?.let { json.encodeToString(it) },
            frontMetricsJson = metadata.frontMetrics?.let { json.encodeToString(it) },
            rightMetricsJson = metadata.rightMetrics?.let { json.encodeToString(it) },
            pdfPath = pdfPath,
            thumbnailPath = thumbnailPath,
            serverId = id,
            syncStatus = "SYNCED",
            version = version,
            userId = metadata.userId,
            lastSyncError = null
        )
    }

    private inline fun <reified T> decodeOrNull(raw: String): T? =
        runCatching { json.decodeFromString<T>(raw) }.getOrNull()

    private fun File.guessImageMimeType(): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith("png") -> "image/png"
            lower.endsWith("webp") -> "image/webp"
            lower.endsWith("heic") -> "image/heic"
            else -> "image/jpeg"
        }
    }
}

