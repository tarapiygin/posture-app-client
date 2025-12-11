package com.example.postureapp.domain.analysis.right

import androidx.compose.ui.geometry.Offset
import com.example.postureapp.domain.analysis.landmarks.AnatomicalPoint
import com.example.postureapp.domain.analysis.landmarks.Landmark
import com.example.postureapp.domain.analysis.landmarks.LandmarkSet
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot

class ComputeRightMetricsUseCase @Inject constructor() {

    operator fun invoke(set: LandmarkSet): RightMetrics? {
        val normalized = set
        val width = normalized.imageWidth.toFloat().takeIf { it > 0f } ?: return null
        val height = normalized.imageHeight.toFloat().takeIf { it > 0f } ?: return null

        val map = normalized.points.associateBy { it.point }

        fun point(name: AnatomicalPoint): Landmark? = map[name]
        fun toPx(landmark: Landmark): Offset = Offset(landmark.x * width, landmark.y * height)

        val ankle = point(AnatomicalPoint.RIGHT_ANKLE) ?: return null
        val ear = point(AnatomicalPoint.RIGHT_EAR) ?: return null
        val c7 = point(AnatomicalPoint.RIGHT_C7) ?: return null

        val anklePx = toPx(ankle)
        val earPx = toPx(ear)
        val c7Px = toPx(c7)

        val bodyVector = earPx - anklePx
        val bodyAngle = abs(angleBetween(bodyVector, Offset(0f, -1f)))

        val cvaVector = earPx - c7Px
        val cva = abs(angleBetween(normalize(cvaVector), Offset(1f, 0f)))

        val knee = point(AnatomicalPoint.RIGHT_KNEE)
        val hip = point(AnatomicalPoint.RIGHT_HIP)
        val shoulder = point(AnatomicalPoint.RIGHT_SHOULDER)
        val chainPoints = listOfNotNull(
            anklePx,
            hip?.let(::toPx),
            shoulder?.let(::toPx),
            earPx
        )

        val segments = listOfNotNull(
            knee?.let { segment("Knee", toPx(it), anklePx) },
            hip?.let { segment("Hip", toPx(it), anklePx) },
            shoulder?.let { segment("Shoulder", toPx(it), anklePx) },
            ear.let { segment("Ear", earPx, anklePx) }
        )

        return RightMetrics(
            bodyAngleDeg = bodyAngle,
            cvaDeg = cva,
            greenBase = anklePx,
            earPx = earPx,
            c7Px = c7Px,
            chainPoints = chainPoints,
            segments = segments
        )
    }

    private fun segment(name: String, point: Offset, ankle: Offset): SegmentAngle {
        val vector = point - ankle
        val angle = abs(angleBetween(vector, Offset(0f, -1f)))
        return SegmentAngle(name = name, angleDeg = angle, anchorPx = point)
    }

    private fun angleBetween(v1: Offset, v2: Offset): Float {
        val dot = v1.x * v2.x + v1.y * v2.y
        val mag1 = magnitude(v1)
        val mag2 = magnitude(v2)
        if (mag1 == 0f || mag2 == 0f) return 0f
        val cos = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return acos(cos) * RAD_TO_DEG
    }

    private fun normalize(vector: Offset): Offset {
        val len = magnitude(vector)
        if (len < 1e-6f) return Offset.Zero
        return Offset(vector.x / len, vector.y / len)
    }

    private fun LandmarkSet.hasRightSynthetic(): Boolean {
        val map = points.associateBy { it.point }
        return map.containsKey(com.example.postureapp.domain.analysis.landmarks.AnatomicalPoint.RIGHT_C7) &&
            map.containsKey(com.example.postureapp.domain.analysis.landmarks.AnatomicalPoint.TIBIAL_TUBEROSITY_RIGHT)
    }

    private fun magnitude(v: Offset): Float = hypot(v.x, v.y)

    private operator fun Offset.minus(other: Offset): Offset = Offset(x - other.x, y - other.y)

    companion object {
        private const val RAD_TO_DEG = 180f / Math.PI.toFloat()
    }
}
