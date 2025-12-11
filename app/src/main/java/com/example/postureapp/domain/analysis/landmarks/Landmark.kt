package com.example.postureapp.domain.analysis.landmarks

data class Landmark(
    val point: AnatomicalPoint,
    val x: Float,
    val y: Float,
    val z: Float?,
    val visibility: Float?,
    val editable: Boolean,
    val code: String
)

