package com.example.postureapp.domain.metrics

import androidx.compose.ui.geometry.Offset
import com.example.postureapp.domain.landmarks.AnatomicalPoint
import com.example.postureapp.domain.landmarks.Landmark
import com.example.postureapp.domain.landmarks.LandmarkSet
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class ComputeFrontMetricsUseCase @Inject constructor() {

    operator fun invoke(set: LandmarkSet): FrontMetrics? {
        val width = set.imageWidth.toFloat().takeIf { it > 0f } ?: return null
        val height = set.imageHeight.toFloat().takeIf { it > 0f } ?: return null

        val map = set.points.associateBy { it.point }

        fun point(name: AnatomicalPoint): Landmark? = map[name]
        fun toPx(landmark: Landmark): Offset =
            Offset(landmark.x * width, landmark.y * height)

        val leftAnkle = point(AnatomicalPoint.LEFT_ANKLE)
        val rightAnkle = point(AnatomicalPoint.RIGHT_ANKLE)
        val jugular = point(AnatomicalPoint.JUGULAR_NOTCH)

        if (leftAnkle == null || rightAnkle == null || jugular == null) return null

        val leftAnklePx = toPx(leftAnkle)
        val rightAnklePx = toPx(rightAnkle)
        val jugularPx = toPx(jugular)
        val base = midpoint(leftAnklePx, rightAnklePx)

        val bodyVector = jugularPx - base
        val bodyAngle = abs(angleBetween(bodyVector, Offset(0f, -1f)))

        val pairs = listOf(
            Triple("Ears", AnatomicalPoint.LEFT_EAR, AnatomicalPoint.RIGHT_EAR),
            Triple("Shoulders", AnatomicalPoint.LEFT_SHOULDER, AnatomicalPoint.RIGHT_SHOULDER),
            Triple("ASIS", AnatomicalPoint.LEFT_HIP, AnatomicalPoint.RIGHT_HIP),
            Triple("Knees", AnatomicalPoint.TIBIAL_TUBEROSITY_LEFT, AnatomicalPoint.TIBIAL_TUBEROSITY_RIGHT),
            Triple("Feet", AnatomicalPoint.LEFT_ANKLE, AnatomicalPoint.RIGHT_ANKLE)
        )

        val rawLevels = pairs.mapNotNull { (name, left, right) ->
            val l = point(left)
            val r = point(right)
            if (l == null || r == null) return@mapNotNull null
            val lPx = toPx(l)
            val rPx = toPx(r)
            val midPx = midpoint(lPx, rPx)
            val y = (lPx.y + rPx.y) / 2f
            val angle = atan2(rPx.y - lPx.y, rPx.x - lPx.x) * RAD_TO_DEG
            val angleAbs = abs(angle)
            val deviation = min(angleAbs, abs(180f - angleAbs)) // deviation from ideal 180°
            LevelAngle(
                name = name,
                deviationDeg = deviation,
                bodyDeviationDeg = 0f, // placeholder, will be filled below
                yPx = y,
                left = lPx,
                right = rPx,
                mid = midPx
            )
        }

        // Body deviation per level: use intersection of red deviation line with current level,
        // and previous level on the green vertical line. Start from base (ankles).
        val dir = (jugularPx - base).let { v ->
            val len = magnitude(v)
            if (len < 1e-6f) Offset(0f, -1f) else Offset(v.x / len, v.y / len)
        }
        // Sort from bottom to top (y increases downward), starting at ankles (base)
        var prevY = base.y
        val levels = rawLevels.sortedByDescending { it.yPx }.map { level ->
            val bodyDeviation = if (level.name == "Feet") {
                null
            } else if (abs(dir.y) < 1e-6f) {
                bodyAngle
            } else {
                val t = (level.yPx - base.y) / dir.y
                val devPoint = Offset(base.x + dir.x * t, base.y + dir.y * t)
                val vertPrev = Offset(base.x, prevY)
                val vec = devPoint - vertPrev
                abs(angleBetween(vec, Offset(0f, -1f)))
            }
            prevY = level.yPx
            level.copy(bodyDeviationDeg = bodyDeviation)
        }.sortedBy { it.yPx } // restore top-down order for drawing

        return FrontMetrics(
            bodyAngleDeg = bodyAngle,
            bodyBase = base,
            jnPx = jugularPx,
            levelAngles = levels
        )
    }

    private fun midpoint(a: Offset, b: Offset): Offset =
        Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    private fun angleBetween(v1: Offset, v2: Offset): Float {
        val dot = v1.x * v2.x + v1.y * v2.y
        val mag1 = magnitude(v1)
        val mag2 = magnitude(v2)
        if (mag1 == 0f || mag2 == 0f) return 0f
        val cos = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return acos(cos) * RAD_TO_DEG
    }

    private fun magnitude(v: Offset): Float = sqrt(v.x.pow(2) + v.y.pow(2))

    companion object {
        private const val RAD_TO_DEG = (180f / Math.PI.toFloat())
    }
}

