package com.example.postureapp.domain.analysis.landmarks

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.postureapp.R

enum class AnatomicalPoint(
    @StringRes val labelRes: Int,
    val overlayCode: String,
    val editable: Boolean,
    val synthetic: Boolean,
    @DrawableRes val referenceDrawableRes: Int,
    @StringRes val helpTextRes: Int,
) {
    LEFT_ANKLE(
        labelRes = R.string.point_left_ankle,
        overlayCode = "LA",
        editable = true,
        referenceDrawableRes = R.drawable.ref_left_ankle,
        helpTextRes = R.string.help_left_ankle,
        synthetic = false

    ),
    RIGHT_ANKLE(
        labelRes = R.string.point_right_ankle,
        overlayCode = "RA",
        editable = true,
        referenceDrawableRes = R.drawable.ref_right_ankle,
        helpTextRes = R.string.help_right_ankle,
        synthetic = false
    ),
    LEFT_KNEE(
        labelRes = R.string.point_left_knee,
        overlayCode = "LK",
        editable = true,
        referenceDrawableRes = R.drawable.ref_left_knee,
        helpTextRes = R.string.help_left_knee,
        synthetic = false
    ),
    RIGHT_KNEE(
        labelRes = R.string.point_right_knee,
        overlayCode = "RK",
        editable = true,
        referenceDrawableRes = R.drawable.ref_right_knee,
        helpTextRes = R.string.help_right_knee,
        synthetic = false
    ),
    LEFT_HIP(
        labelRes = R.string.point_left_hip,
        overlayCode = "LH",
        editable = true,
        referenceDrawableRes = R.drawable.ref_left_hip,
        helpTextRes = R.string.help_left_hip,
        synthetic = false
    ),
    RIGHT_HIP(
        labelRes = R.string.point_right_hip,
        overlayCode = "RH",
        editable = true,
        referenceDrawableRes = R.drawable.ref_right_hip,
        helpTextRes = R.string.help_right_hip,
        synthetic = false
    ),
    LEFT_SHOULDER(
        labelRes = R.string.point_left_shoulder,
        overlayCode = "LS",
        editable = true,
        referenceDrawableRes = R.drawable.ref_left_shoulder,
        helpTextRes = R.string.help_left_shoulder,
        synthetic = false
    ),
    RIGHT_SHOULDER(
        labelRes = R.string.point_right_shoulder,
        overlayCode = "RS",
        editable = true,
        referenceDrawableRes = R.drawable.ref_right_shoulder,
        helpTextRes = R.string.help_right_shoulder,
        synthetic = false
    ),
    LEFT_EAR(
        labelRes = R.string.point_left_ear,
        overlayCode = "LE",
        editable = true,
        referenceDrawableRes = R.drawable.ref_left_ear,
        helpTextRes = R.string.help_left_ear,
        synthetic = false
    ),
    RIGHT_EAR(
        labelRes = R.string.point_right_ear,
        overlayCode = "RE",
        editable = true,
        referenceDrawableRes = R.drawable.ref_right_ear,
        helpTextRes = R.string.help_right_ear,
        synthetic = false
    ),
    RIGHT_C7(
        labelRes = R.string.point_right_c7,
        overlayCode = "C7",
        editable = true,
        referenceDrawableRes = R.drawable.ref_right_c7,
        helpTextRes = R.string.help_right_c7,
        synthetic = true
    ),
    TIBIAL_TUBEROSITY_LEFT(
        labelRes = R.string.point_tibial_tuberosity_left,
        overlayCode = "TTL",
        editable = true,
        referenceDrawableRes = R.drawable.ref_left_knee,
        helpTextRes = R.string.help_tibial_tuberosity_left,
        synthetic = true
    ),
    TIBIAL_TUBEROSITY_RIGHT(
        labelRes = R.string.point_tibial_tuberosity_right,
        overlayCode = "TTR",
        editable = true,
        referenceDrawableRes = R.drawable.ref_right_knee,
        helpTextRes = R.string.help_tibial_tuberosity_right,
        synthetic = true
    ),
    JUGULAR_NOTCH(
        labelRes = R.string.point_jugular_notch,
        overlayCode = "JN",
        editable = true,
        referenceDrawableRes = R.drawable.ref_left_shoulder,
        helpTextRes = R.string.help_jugular_notch,
        synthetic = true
    );

    companion object {
        val BasePointsAll: List<AnatomicalPoint> = entries.filter { !it.synthetic }
        val BasePointsEditable: List<AnatomicalPoint> = entries.filter { it.editable }
        val SyntheticPoints: List<AnatomicalPoint> = entries.filter { it.synthetic }
        // Points shown on front editing screen (all editable)
        val FrontVisiblePoints: List<AnatomicalPoint> = listOf(
            TIBIAL_TUBEROSITY_LEFT,
            TIBIAL_TUBEROSITY_RIGHT,
            JUGULAR_NOTCH,
            LEFT_EAR,
            RIGHT_EAR,
            LEFT_SHOULDER,
            RIGHT_SHOULDER,
            LEFT_HIP,
            RIGHT_HIP,
            LEFT_ANKLE,
            RIGHT_ANKLE
        )
    }
}
