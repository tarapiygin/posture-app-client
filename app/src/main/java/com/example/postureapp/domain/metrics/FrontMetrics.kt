package com.example.postureapp.domain.metrics

import androidx.compose.ui.geometry.Offset

data class LevelAngle(
    val name: String,
    val deviationDeg: Float,
    val bodyDeviationDeg: Float?,
    val yPx: Float,
    val left: Offset,
    val right: Offset,
    val mid: Offset
)

data class FrontMetrics(
    val bodyAngleDeg: Float,
    val bodyBase: Offset,   // M_ankles in px
    val jnPx: Offset,
    val levelAngles: List<LevelAngle>
)

