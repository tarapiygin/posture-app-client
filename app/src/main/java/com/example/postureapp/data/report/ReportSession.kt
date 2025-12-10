package com.example.postureapp.data.report

import com.example.postureapp.core.report.Side

data class SideState(
    val side: Side,
    val originalPath: String? = null,
    val croppedPath: String? = null,
    val resultId: String? = null,
    val hasAuto: Boolean = false,
    val hasFinal: Boolean = false
)

data class ReportSession(
    val id: String,
    val front: SideState = SideState(Side.FRONT),
    val right: SideState = SideState(Side.RIGHT)
)

