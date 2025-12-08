package com.example.postureapp.ui.edit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private object LandmarkPointStyles {
    val baseRadius = 8.dp
    val activeRadius = 14.dp
    val syntheticRadius = 7.dp
    val strokeWidth = 2.dp
    val dashPattern = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
}

fun DrawScope.drawLandmarkPoint(
    position: Offset,
    active: Boolean,
    editable: Boolean,
    accent: Color
) {
    val strokeWidth = LandmarkPointStyles.strokeWidth.toPx()
    if (editable) {
        val radius = (if (active) LandmarkPointStyles.activeRadius else LandmarkPointStyles.baseRadius).toPx()
        drawCircle(
            color = accent,
            radius = radius,
            center = position
        )
        drawCircle(
            color = Color.White,
            radius = radius,
            center = position,
            style = Stroke(width = strokeWidth)
        )
    } else {
        val radius = LandmarkPointStyles.syntheticRadius.toPx()
        drawCircle(
            color = accent,
            radius = radius,
            center = position,
            style = Stroke(width = strokeWidth, pathEffect = LandmarkPointStyles.dashPattern)
        )
    }
}


