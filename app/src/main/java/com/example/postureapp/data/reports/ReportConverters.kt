package com.example.postureapp.data.reports

import androidx.compose.ui.geometry.Offset
import androidx.room.TypeConverter
import com.example.postureapp.domain.landmarks.AnatomicalPoint
import com.example.postureapp.domain.landmarks.Landmark
import com.example.postureapp.domain.landmarks.LandmarkSet
import com.example.postureapp.domain.metrics.FrontMetrics
import com.example.postureapp.domain.metrics.LevelAngle
import com.example.postureapp.domain.metrics.right.RightMetrics
import com.example.postureapp.domain.metrics.right.SegmentAngle
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReportConverters(
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false }
) {

    @TypeConverter
    fun encodeLandmarks(value: LandmarkSet?): String? {
        if (value == null) return null
        return json.encodeToString(value.toStored())
    }

    @TypeConverter
    fun decodeLandmarks(raw: String?): LandmarkSet? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(StoredLandmarkSet.serializer(), raw) }
            .getOrNull()
            ?.toDomain()
    }

    @TypeConverter
    fun encodeFrontMetrics(value: FrontMetrics?): String? {
        if (value == null) return null
        return json.encodeToString(value.toStored())
    }

    @TypeConverter
    fun decodeFrontMetrics(raw: String?): FrontMetrics? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(StoredFrontMetrics.serializer(), raw) }
            .getOrNull()
            ?.toDomain()
    }

    @TypeConverter
    fun encodeRightMetrics(value: RightMetrics?): String? {
        if (value == null) return null
        return json.encodeToString(value.toStored())
    }

    @TypeConverter
    fun decodeRightMetrics(raw: String?): RightMetrics? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString(StoredRightMetrics.serializer(), raw) }
            .getOrNull()
            ?.toDomain()
    }
}

@Serializable
private data class StoredLandmark(
    val point: String,
    val x: Float,
    val y: Float,
    val z: Float? = null,
    val visibility: Float? = null,
    val editable: Boolean,
    val code: String
)

@Serializable
private data class StoredLandmarkSet(
    val imageWidth: Int,
    val imageHeight: Int,
    val points: List<StoredLandmark>
)

@Serializable
private data class StoredOffset(val x: Float, val y: Float)

@Serializable
private data class StoredLevelAngle(
    val name: String,
    val deviationDeg: Float,
    val bodyDeviationDeg: Float? = null,
    val yPx: Float,
    val left: StoredOffset,
    val right: StoredOffset,
    val mid: StoredOffset
)

@Serializable
private data class StoredFrontMetrics(
    val bodyAngleDeg: Float,
    val bodyBase: StoredOffset,
    val jnPx: StoredOffset,
    val levelAngles: List<StoredLevelAngle>
)

@Serializable
private data class StoredSegment(
    val name: String,
    val angleDeg: Float,
    val anchor: StoredOffset
)

@Serializable
private data class StoredRightMetrics(
    val bodyAngleDeg: Float,
    val cvaDeg: Float,
    val greenBase: StoredOffset,
    val earPx: StoredOffset,
    val c7Px: StoredOffset,
    val chainPoints: List<StoredOffset>,
    val segments: List<StoredSegment>
)

private fun LandmarkSet.toStored(): StoredLandmarkSet =
    StoredLandmarkSet(
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        points = points.map {
            StoredLandmark(
                point = it.point.name,
                x = it.x,
                y = it.y,
                z = it.z,
                visibility = it.visibility,
                editable = it.editable,
                code = it.code
            )
        }
    )

private fun StoredLandmarkSet.toDomain(): LandmarkSet {
    val mapped = points.mapNotNull { lm ->
        val anatomical = runCatching { AnatomicalPoint.valueOf(lm.point) }.getOrNull() ?: return@mapNotNull null
        Landmark(
            point = anatomical,
            x = lm.x,
            y = lm.y,
            z = lm.z,
            visibility = lm.visibility,
            editable = lm.editable,
            code = lm.code
        )
    }
    return LandmarkSet(
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        points = mapped
    )
}

private fun FrontMetrics.toStored(): StoredFrontMetrics =
    StoredFrontMetrics(
        bodyAngleDeg = bodyAngleDeg,
        bodyBase = bodyBase.toStored(),
        jnPx = jnPx.toStored(),
        levelAngles = levelAngles.map { level ->
            StoredLevelAngle(
                name = level.name,
                deviationDeg = level.deviationDeg,
                bodyDeviationDeg = level.bodyDeviationDeg,
                yPx = level.yPx,
                left = level.left.toStored(),
                right = level.right.toStored(),
                mid = level.mid.toStored()
            )
        }
    )

private fun StoredFrontMetrics.toDomain(): FrontMetrics =
    FrontMetrics(
        bodyAngleDeg = bodyAngleDeg,
        bodyBase = bodyBase.toDomain(),
        jnPx = jnPx.toDomain(),
        levelAngles = levelAngles.map {
            LevelAngle(
                name = it.name,
                deviationDeg = it.deviationDeg,
                bodyDeviationDeg = it.bodyDeviationDeg,
                yPx = it.yPx,
                left = it.left.toDomain(),
                right = it.right.toDomain(),
                mid = it.mid.toDomain()
            )
        }
    )

private fun RightMetrics.toStored(): StoredRightMetrics =
    StoredRightMetrics(
        bodyAngleDeg = bodyAngleDeg,
        cvaDeg = cvaDeg,
        greenBase = greenBase.toStored(),
        earPx = earPx.toStored(),
        c7Px = c7Px.toStored(),
        chainPoints = chainPoints.map { it.toStored() },
        segments = segments.map {
            StoredSegment(
                name = it.name,
                angleDeg = it.angleDeg,
                anchor = it.anchorPx.toStored()
            )
        }
    )

private fun StoredRightMetrics.toDomain(): RightMetrics =
    RightMetrics(
        bodyAngleDeg = bodyAngleDeg,
        cvaDeg = cvaDeg,
        greenBase = greenBase.toDomain(),
        earPx = earPx.toDomain(),
        c7Px = c7Px.toDomain(),
        chainPoints = chainPoints.map { it.toDomain() },
        segments = segments.map {
            SegmentAngle(
                name = it.name,
                angleDeg = it.angleDeg,
                anchorPx = it.anchor.toDomain()
            )
        }
    )

private fun Offset.toStored(): StoredOffset = StoredOffset(x, y)
private fun StoredOffset.toDomain(): Offset = Offset(x, y)

