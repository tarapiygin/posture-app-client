package com.example.postureapp.ui.capture.photo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.postureapp.R
import com.example.postureapp.ui.designsystem.AccentBlue
import com.example.postureapp.ui.designsystem.SuccessGreen
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HorizonOverlay(
    pitch: Float,
    roll: Float,
    aligned: Boolean,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val rollAligned = abs(roll) <= PhotoCaptureViewModel.ROLL_TOLERANCE
    val pitchAligned = abs(pitch) <= PhotoCaptureViewModel.PITCH_TOLERANCE
    val horizonColor = if (aligned) SuccessGreen else AccentBlue
    val hintMessages = buildList {
        if (!rollAligned) {
            add(
                if (roll > PhotoCaptureViewModel.ROLL_TOLERANCE) {
                    R.string.analysis_hint_tilt_left
                } else {
                    R.string.analysis_hint_tilt_right
                }
            )
        }
        if (!pitchAligned) {
            add(
                if (pitch > PhotoCaptureViewModel.PITCH_TOLERANCE) {
                    R.string.analysis_hint_lower
                } else {
                    R.string.analysis_hint_raise
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            rotate(-roll, center) {
                drawLine(
                    color = horizonColor.copy(alpha = 0.9f),
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            val crosshairRadius = 18.dp.toPx()
            drawLine(
                color = horizonColor.copy(alpha = 0.7f),
                start = Offset(center.x - crosshairRadius, center.y),
                end = Offset(center.x + crosshairRadius, center.y),
                strokeWidth = strokeWidth / 2,
                cap = StrokeCap.Round
            )
            drawLine(
                color = horizonColor.copy(alpha = 0.7f),
                start = Offset(center.x, center.y - crosshairRadius),
                end = Offset(center.x, center.y + crosshairRadius),
                strokeWidth = strokeWidth / 2,
                cap = StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 32.dp,
                    bottom = 32.dp + bottomPadding
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReadoutBadge(
                    labelRes = R.string.analysis_pitch_label,
                    value = pitch
                )
                ReadoutBadge(
                    labelRes = R.string.analysis_roll_label,
                    value = roll
                )
            }

            if (hintMessages.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge)
                        .padding(vertical = 20.dp, horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hintMessages.forEach { hint ->
                        Text(
                            text = stringResource(hint),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AccentBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadoutBadge(
    labelRes: Int,
    value: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${value.roundToInt()}\u00B0",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
