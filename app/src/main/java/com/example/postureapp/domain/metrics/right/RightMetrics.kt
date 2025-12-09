package com.example.postureapp.domain.metrics.right

import androidx.compose.ui.geometry.Offset

data class SegmentAngle(
    val name: String,
    val angleDeg: Float,
    val anchorPx: Offset
)

data class RightMetrics(
    val bodyAngleDeg: Float,
    val cvaDeg: Float,
    val greenBase: Offset,
    val earPx: Offset,
    val c7Px: Offset,
    val chainPoints: List<Offset>,
    val segments: List<SegmentAngle>
)
