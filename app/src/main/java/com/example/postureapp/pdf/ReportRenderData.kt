package com.example.postureapp.pdf

import com.example.postureapp.core.draw.FrontLevel
import com.example.postureapp.core.draw.RightSegment
import com.example.postureapp.domain.landmarks.LandmarkSet
import com.example.postureapp.domain.metrics.FrontMetrics
import com.example.postureapp.domain.metrics.right.RightMetrics

data class ReportRenderData(
    val sessionId: String,
    val createdAt: Long,
    val front: FrontRenderData?,
    val right: RightRenderData?
)

data class FrontRenderData(
    val imagePath: String,
    val landmarks: LandmarkSet,
    val metrics: FrontMetrics
)

data class RightRenderData(
    val imagePath: String,
    val landmarks: LandmarkSet,
    val metrics: RightMetrics
)

data class FrontTile(
    val level: FrontLevel,
    val angleDeg: Float
)

data class RightTile(
    val segment: RightSegment,
    val angleDeg: Float
)

