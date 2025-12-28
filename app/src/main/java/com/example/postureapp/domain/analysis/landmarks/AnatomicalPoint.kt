package com.example.postureapp.domain.analysis.landmarks

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.postureapp.R
import com.example.postureapp.core.analysis.Side

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
        referenceDrawableRes = R.drawable.ref_front_side_left_ankle,
        helpTextRes = R.string.help_left_ankle,
        synthetic = false

    ),
    RIGHT_ANKLE(
        labelRes = R.string.point_right_ankle,
        overlayCode = "RA",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_right_ankle,
        helpTextRes = R.string.help_right_ankle,
        synthetic = false
    ),
    LEFT_KNEE(
        labelRes = R.string.point_left_knee,
        overlayCode = "LK",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_left_ankle,
        helpTextRes = R.string.help_left_knee,
        synthetic = false
    ),
    RIGHT_KNEE(
        labelRes = R.string.point_right_knee,
        overlayCode = "RK",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_right_knee,
        helpTextRes = R.string.help_right_knee,
        synthetic = false
    ),
    LEFT_HIP(
        labelRes = R.string.point_left_hip,
        overlayCode = "LH",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_left_hip,
        helpTextRes = R.string.help_left_hip,
        synthetic = false
    ),
    RIGHT_HIP(
        labelRes = R.string.point_right_hip,
        overlayCode = "RH",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_right_hip,
        helpTextRes = R.string.help_right_hip,
        synthetic = false
    ),
    LEFT_SHOULDER(
        labelRes = R.string.point_left_shoulder,
        overlayCode = "LS",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_left_shoulder,
        helpTextRes = R.string.help_left_shoulder,
        synthetic = false
    ),
    RIGHT_SHOULDER(
        labelRes = R.string.point_right_shoulder,
        overlayCode = "RS",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_right_shoulder,
        helpTextRes = R.string.help_right_shoulder,
        synthetic = false
    ),
    LEFT_EAR(
        labelRes = R.string.point_left_ear,
        overlayCode = "LE",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_left_ear,
        helpTextRes = R.string.help_left_ear,
        synthetic = false
    ),
    RIGHT_EAR(
        labelRes = R.string.point_right_ear,
        overlayCode = "RE",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_right_ear,
        helpTextRes = R.string.help_right_ear,
        synthetic = false
    ),
    RIGHT_C7(
        labelRes = R.string.point_right_c7,
        overlayCode = "C7",
        editable = true,
        referenceDrawableRes = R.drawable.ref_right_side_right_c7,
        helpTextRes = R.string.help_right_c7,
        synthetic = true
    ),
    TIBIAL_TUBEROSITY_LEFT(
        labelRes = R.string.point_tibial_tuberosity_left,
        overlayCode = "TTL",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_tibial_tuberosity_left,
        helpTextRes = R.string.help_tibial_tuberosity_left,
        synthetic = true
    ),
    TIBIAL_TUBEROSITY_RIGHT(
        labelRes = R.string.point_tibial_tuberosity_right,
        overlayCode = "TTR",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_tibial_tuberosity_right,
        helpTextRes = R.string.help_tibial_tuberosity_right,
        synthetic = true
    ),
    JUGULAR_NOTCH(
        labelRes = R.string.point_jugular_notch,
        overlayCode = "JN",
        editable = true,
        referenceDrawableRes = R.drawable.ref_front_side_jugular_notch,
        helpTextRes = R.string.help_jugular_notch,
        synthetic = true
    );

    /**
     * Возвращает ресурс изображения для данной точки в зависимости от вида (фронт/правая сторона).
     *
     * Организация файлов:
     * - Фронтальные изображения: ref_front_side_left_ankle, ref_front_side_right_ankle и т.д.
     * - Изображения правой стороны: ref_right_side_right_ankle, ref_right_side_right_knee и т.д.
     *
     * Примечание: Android не поддерживает поддиректории в res/drawable/,
     * поэтому используется префиксная система именования файлов.
     */
    fun getReferenceDrawable(side: Side): Int {
        return when (side) {
            Side.FRONT -> referenceDrawableRes
            Side.RIGHT -> when (this) {
                RIGHT_ANKLE -> R.drawable.ref_right_side_right_ankle
                RIGHT_KNEE -> R.drawable.ref_right_side_right_knee
                RIGHT_HIP -> R.drawable.ref_right_side_right_hip
                RIGHT_SHOULDER -> R.drawable.ref_right_side_right_shoulder
                RIGHT_EAR -> R.drawable.ref_right_side_right_ear
                RIGHT_C7 -> R.drawable.ref_right_side_right_c7
                // Для всех остальных точек используем фронтальное изображение как запасной вариант
                else -> referenceDrawableRes
            }
        }
    }

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
        /** Точки, видимые и редактируемые на правом экране */
        val RightSidePoints: List<AnatomicalPoint> = listOf(
                RIGHT_ANKLE,
                RIGHT_KNEE,
                RIGHT_HIP,
                RIGHT_SHOULDER,
                RIGHT_EAR,
                RIGHT_C7
        )
    }
}
