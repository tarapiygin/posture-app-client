package com.example.postureapp.data.reports.sync.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReportUploadDto(
    val clientId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sessionId: String? = null,
    val sessionClientId: String,
    val userId: String? = null,
    val frontImagePath: String? = null,
    val rightImagePath: String? = null,
    val frontLandmarks: LandmarkSetDto? = null,
    val rightLandmarks: LandmarkSetDto? = null,
    val frontMetrics: FrontMetricsDto? = null,
    val rightMetrics: RightMetricsDto? = null,
    val assets: ReportAssetsPayload = ReportAssetsPayload(),
    val version: Int = 1
)

@Serializable
data class ReportAssetRefDto(
    val serverId: String,
    val side: String,
    val kind: String
)

@Serializable
data class ReportAssetsPayload(
    val front: List<ReportAssetRefDto> = emptyList(),
    val right: List<ReportAssetRefDto> = emptyList()
)

@Serializable
data class SessionResolveRequest(
    val sessionId: String? = null,
    val sessionClientId: String
)

@Serializable
data class SessionResolveResponse(
    val sessionId: String
)

@Serializable
data class PhotoUploadResponse(val id: String? = null)

@Serializable
data class ReportsDeltaRequest(
    val knownServerIds: List<String> = emptyList(),
    val since: Long
)

@Serializable
data class ReportsDeltaResponse(
    val addedOrUpdated: List<RemoteReportItem>,
    val deletedServerIds: List<String> = emptyList()
)

@Serializable
data class RemoteReportItem(
    val id: String,
    val version: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val metadata: ReportUploadDto,
    val pdfUrl: String
)

@Serializable
data class ReportUploadResponse(
    val id: String,
    val version: Int? = null
)

@Serializable
data class AssetUploadMetadata(
    val clientId: String,
    val reportClientId: String,
    val side: String,
    val kind: String,
    val sha256: String?,
    val width: Int?,
    val height: Int?,
    val createdAt: Long
)

@Serializable
data class LandmarkSetDto(
    val imageWidth: Int,
    val imageHeight: Int,
    val points: List<LandmarkDto>
)

@Serializable
data class LandmarkDto(
    val point: String,
    val x: Float,
    val y: Float,
    val z: Float? = null,
    val visibility: Float? = null,
    val editable: Boolean,
    val code: String
)

@Serializable
data class FrontMetricsDto(
    val bodyAngleDeg: Float,
    val bodyBase: OffsetDto,
    val jnPx: OffsetDto,
    val levelAngles: List<LevelAngleDto>
)

@Serializable
data class LevelAngleDto(
    val name: String,
    val deviationDeg: Float,
    val bodyDeviationDeg: Float? = null,
    val yPx: Float,
    val left: OffsetDto,
    val right: OffsetDto,
    val mid: OffsetDto
)

@Serializable
data class RightMetricsDto(
    val bodyAngleDeg: Float,
    val cvaDeg: Float,
    val greenBase: OffsetDto,
    val earPx: OffsetDto,
    val c7Px: OffsetDto,
    val chainPoints: List<OffsetDto>,
    val segments: List<SegmentAngleDto>
)

@Serializable
data class SegmentAngleDto(
    val name: String,
    val angleDeg: Float,
    val anchor: OffsetDto
)

@Serializable
data class OffsetDto(
    val x: Float,
    val y: Float
)

