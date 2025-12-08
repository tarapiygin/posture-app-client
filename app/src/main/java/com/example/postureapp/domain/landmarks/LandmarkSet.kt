package com.example.postureapp.domain.landmarks

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

    fun recomputeSynthetic(): LandmarkSet {
        // Собираем карту ВСЕХ базовых точек, которые нужны для вычислений
        val baseMap = baseAll().associateBy { it.point }
        // Если не все необходимые базовые точки есть — лучше не трогать набор
        if (baseMap.size < AnatomicalPoint.BasePointsAll.size) return this
        // Считаем синтетические точки из baseMap
        val synthetic = computeSynthetic(baseMap)
        // Берём только те базовые точки, которые должны быть видимыми в UI
        val visibleBase = AnatomicalPoint.BasePointsEditable.mapNotNull { baseMap[it] }
        // Добавляем синтетические точки в заданном порядке
        val orderedSynthetic = AnatomicalPoint.SyntheticPoints.mapNotNull { synthetic[it] }
        // Возвращаем новый LandmarkSet: только видимые базовые + синтетика
        return copy(points = visibleBase + orderedSynthetic)
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