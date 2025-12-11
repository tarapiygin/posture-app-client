package com.example.postureapp.domain.analysis.landmarks

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

data class LandmarkSet(
    val imageWidth: Int,
    val imageHeight: Int,
    val points: List<Landmark>
) {

    fun withUpdated(name: String, x: Float, y: Float): LandmarkSet {
        val target = runCatching { AnatomicalPoint.valueOf(name) }.getOrNull()
            ?: return this
        if (!target.editable) return this
        val clampedX = x.coerceIn(0f, 1f)
        val clampedY = y.coerceIn(0f, 1f)
        val updated = points.map { landmark ->
            if (landmark.point == target) {
                landmark.copy(x = clampedX, y = clampedY)
            } else {
                landmark
            }
        }
        // БЫЛО:
        // return copy(points = updated).recomputeSynthetic()

        // СТАЛО:
        // просто сохраняем новые координаты, без пересчёта синтетики
        return copy(points = updated)
    }

    private fun baseAll(): List<Landmark> =
        AnatomicalPoint.BasePointsAll.mapNotNull { point ->
            points.firstOrNull { it.point == point }
        }

    /**
     * Набор для фронт-экрана: только нужные точки, все редактируемые.
     * Синтетика (TTL/TTR/JN) вычисляется один раз, если отсутствует.
     */
    fun toFrontSide(): LandmarkSet {
        val baseMap = points.associateBy { it.point }
        val synthetic = computeFrontSynthetic(baseMap)
        val all = mergePoints(synthetic.values)
        val visible = AnatomicalPoint.FrontVisiblePoints.mapNotNull { all.findPoint(it) }
        return copy(points = visible)
    }

    /**
     * Набор для правого экрана: RA, RK, RH, RS, RE, C7.
     * C7 вычисляется один раз, если отсутствует.
     */
    fun toRightSide(): LandmarkSet {
        val baseMap = points.associateBy { it.point }
        val synthetic = computeRightSynthetic(baseMap)
        val all = mergePoints(synthetic.values)
        val visible = RightSidePoints.VisiblePoints.mapNotNull { all.findPoint(it) }
        return copy(points = visible)
    }

    private fun computeSynthetic(base: Map<AnatomicalPoint, Landmark>): Map<AnatomicalPoint, Landmark> {
        val leftAnkle = base.getValue(AnatomicalPoint.LEFT_ANKLE)
        val rightAnkle = base.getValue(AnatomicalPoint.RIGHT_ANKLE)
        val leftKnee = base.getValue(AnatomicalPoint.LEFT_KNEE)
        val rightKnee = base.getValue(AnatomicalPoint.RIGHT_KNEE)
        val leftHip = base.getValue(AnatomicalPoint.LEFT_HIP)
        val rightHip = base.getValue(AnatomicalPoint.RIGHT_HIP)
        val leftShoulder = base.getValue(AnatomicalPoint.LEFT_SHOULDER)
        val rightShoulder = base.getValue(AnatomicalPoint.RIGHT_SHOULDER)

        val tibialLeft = interpolate(leftKnee, leftAnkle, 0.15f, AnatomicalPoint.TIBIAL_TUBEROSITY_LEFT)
        val tibialRight = interpolate(rightKnee, rightAnkle, 0.15f, AnatomicalPoint.TIBIAL_TUBEROSITY_RIGHT)

        val shoulderCenter = midpoint(leftShoulder, rightShoulder)
        val hipCenter = midpoint(leftHip, rightHip)
        val shoulderHipDistance = distance(shoulderCenter, hipCenter)
        val jugularOffset = 0.07f * shoulderHipDistance
        val jugularY = (shoulderCenter.y + jugularOffset).coerceIn(0f, 1f)
        val jugular = createSynthetic(
            point = AnatomicalPoint.JUGULAR_NOTCH,
            x = shoulderCenter.x,
            y = jugularY,
            z = null,
            visibility = combineVisibility(
                leftShoulder.visibility,
                rightShoulder.visibility,
                leftHip.visibility,
                rightHip.visibility
            )
        )

        return mapOf(
            AnatomicalPoint.TIBIAL_TUBEROSITY_LEFT to tibialLeft,
            AnatomicalPoint.TIBIAL_TUBEROSITY_RIGHT to tibialRight,
            AnatomicalPoint.JUGULAR_NOTCH to jugular
        )
    }

    private fun computeFrontSynthetic(base: Map<AnatomicalPoint, Landmark>): Map<AnatomicalPoint, Landmark> {
        val result = mutableMapOf<AnatomicalPoint, Landmark>()
        val leftKnee = base[AnatomicalPoint.LEFT_KNEE]
        val leftAnkle = base[AnatomicalPoint.LEFT_ANKLE]
        if (leftKnee != null && leftAnkle != null) {
            result[AnatomicalPoint.TIBIAL_TUBEROSITY_LEFT] =
                interpolate(leftKnee, leftAnkle, 0.15f, AnatomicalPoint.TIBIAL_TUBEROSITY_LEFT)
        }
        val rightKnee = base[AnatomicalPoint.RIGHT_KNEE]
        val rightAnkle = base[AnatomicalPoint.RIGHT_ANKLE]
        if (rightKnee != null && rightAnkle != null) {
            result[AnatomicalPoint.TIBIAL_TUBEROSITY_RIGHT] =
                interpolate(rightKnee, rightAnkle, 0.15f, AnatomicalPoint.TIBIAL_TUBEROSITY_RIGHT)
        }
        val leftShoulder = base[AnatomicalPoint.LEFT_SHOULDER]
        val rightShoulder = base[AnatomicalPoint.RIGHT_SHOULDER]
        val leftHip = base[AnatomicalPoint.LEFT_HIP]
        val rightHip = base[AnatomicalPoint.RIGHT_HIP]
        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
            val shoulderCenter = midpoint(leftShoulder, rightShoulder)
            val hipCenter = midpoint(leftHip, rightHip)
            val shoulderHipDistance = distance(shoulderCenter, hipCenter)
            val jugularOffset = 0.07f * shoulderHipDistance
            val jugularY = (shoulderCenter.y + jugularOffset).coerceIn(0f, 1f)
            result[AnatomicalPoint.JUGULAR_NOTCH] = createSynthetic(
                point = AnatomicalPoint.JUGULAR_NOTCH,
                x = shoulderCenter.x,
                y = jugularY,
                z = null,
                visibility = combineVisibility(
                    leftShoulder.visibility,
                    rightShoulder.visibility,
                    leftHip.visibility,
                    rightHip.visibility
                )
            )
        }
        return result
    }

    private fun computeRightSynthetic(base: Map<AnatomicalPoint, Landmark>): Map<AnatomicalPoint, Landmark> {
        val result = mutableMapOf<AnatomicalPoint, Landmark>()
        val rightShoulder = base[AnatomicalPoint.RIGHT_SHOULDER]
        val rightEar = base[AnatomicalPoint.RIGHT_EAR]

        if (rightShoulder != null && rightEar != null) {
            val neckLen = distance(rightEar, rightShoulder)

            val shoulder = rightShoulder.toOffset()

            // Для правого профиля:
            // вверх – по y отрицательно, назад – влево по x
            val upDir = Offset(0f, -1f)
            val backDir = Offset(-1f, 0f)

            // Коэффициенты подбираем по фото
            val c7Offset = shoulder +
                    upDir * (0.3f * neckLen) +   // чуть выше плеча
                    backDir * (0.4f * neckLen)   // чуть кзади от плеча

            result[AnatomicalPoint.RIGHT_C7] = createSynthetic(
                point = AnatomicalPoint.RIGHT_C7,
                x = c7Offset.x.coerceIn(0f, 1f),
                y = c7Offset.y.coerceIn(0f, 1f),
                z = null,
                visibility = combineVisibility(
                    rightEar.visibility,
                    rightShoulder.visibility
                )
            )
        }

        return result
    }


    private fun mergePoints(newPoints: Collection<Landmark>): List<Landmark> {
        val map = points.associateBy { it.point }.toMutableMap()
        newPoints.forEach { map[it.point] = it }
        return map.values.toList()
    }

    private fun List<Landmark>.findPoint(point: AnatomicalPoint): Landmark? =
        firstOrNull { it.point == point }
}

private fun interpolate(
    start: Landmark,
    end: Landmark,
    factor: Float,
    point: AnatomicalPoint
): Landmark {
    val x = start.x + factor * (end.x - start.x)
    val y = start.y + factor * (end.y - start.y)
    val z = lerp(start.z, end.z, factor)
    val visibility = combineVisibility(start.visibility, end.visibility)
    return createSynthetic(point, x, y, z, visibility)
}

private fun midpoint(first: Landmark, second: Landmark): Offset {
    return Offset(
        x = (first.x + second.x) / 2f,
        y = (first.y + second.y) / 2f
    )
}

private fun distance(first: Offset, second: Offset): Float {
    return hypot(first.x - second.x, first.y - second.y)
}

private fun distance(first: Landmark, second: Landmark): Float = distance(first.toOffset(), second.toOffset())

private fun lerp(start: Float?, end: Float?, factor: Float): Float? {
    return when {
        start == null && end == null -> null
        start == null -> end
        end == null -> start
        else -> start + factor * (end - start)
    }
}

private fun combineVisibility(vararg values: Float?): Float? {
    val filtered = values.filterNotNull()
    return if (filtered.isEmpty()) null else filtered.minOrNull()
}

private fun createSynthetic(
    point: AnatomicalPoint,
    x: Float,
    y: Float,
    z: Float?,
    visibility: Float?
): Landmark {
    return Landmark(
        point = point,
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        z = z,
        visibility = visibility,
        editable = point.editable,
        code = point.overlayCode
    )
}

private fun Landmark.toOffset(): Offset = Offset(x, y)

private operator fun Offset.minus(other: Offset): Offset = Offset(x - other.x, y - other.y)

private operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)

private operator fun Offset.times(factor: Float): Offset = Offset(x * factor, y * factor)

private fun normalize(vector: Offset): Offset {
    val length = hypot(vector.x, vector.y)
    if (length < 1e-6f) return Offset.Zero
    return Offset(vector.x / length, vector.y / length)
}