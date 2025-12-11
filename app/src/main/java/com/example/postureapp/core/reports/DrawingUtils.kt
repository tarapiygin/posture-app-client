package com.example.postureapp.core.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt

fun DrawScope.drawDottedVertical(
    x: Float,
    color: Color,
    strokeWidth: Dp = 2.dp,
    dashOn: Dp = 10.dp,
    dashOff: Dp = 6.dp
) {
    val strokePx = strokeWidth.toPx()
    drawLine(
        color = color,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = strokePx,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashOn.toPx(), dashOff.toPx()),
            0f
        )
    )
}

fun DrawScope.drawDottedLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Dp = 2.dp,
    dashOn: Dp = 10.dp,
    dashOff: Dp = 6.dp
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashOn.toPx(), dashOff.toPx()),
            0f
        )
    )
}

fun DrawScope.drawDottedPolyline(
    points: List<Offset>,
    color: Color,
    strokeWidth: Dp = 2.dp,
    dashOn: Dp = 8.dp,
    dashOff: Dp = 6.dp
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashOn.toPx(), dashOff.toPx()),
                0f
            )
        )
    )
}

fun DrawScope.drawAngleArc(
    center: Offset,
    startVector: Offset,
    endVector: Offset,
    radius: Dp = 42.dp,
    color: Color,
    strokeWidth: Dp = 3.dp
) {
    val startAngle = atan2(startVector.y, startVector.x) * DEGREES
    val endAngle = atan2(endVector.y, endVector.x) * DEGREES
    val sweep = abs(endAngle - startAngle)
    val start = min(startAngle, endAngle)
    val r = radius.toPx()
    val diameter = r * 2
    drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(diameter, diameter),
        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
    )
}

fun DrawScope.drawLevelGuide(
    y: Float,
    color: Color,
    strokeWidth: Dp = 2.dp
) {
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidth.toPx(),
        cap = StrokeCap.Round
    )
}

fun DrawScope.drawLevelSegment(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Dp = 5.dp
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth.toPx(),
        cap = StrokeCap.Round
    )
}

@Composable
fun LevelLabelChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun DegreePill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            ),
            color = color
        )
    }
}

fun Modifier.positionAt(
    xPx: Float,
    yPx: Float,
    parentWidth: Float,
    alignEnd: Boolean = false
): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val x = if (alignEnd) {
        (parentWidth - xPx - placeable.width).roundToInt()
    } else {
        xPx.roundToInt()
    }
    val y = (yPx - placeable.height / 2f).roundToInt()
    val maxWidth = constraints.maxWidth
    val maxHeight = constraints.maxHeight
    val finalX = x.coerceIn(0, maxWidth - placeable.width)
    val finalY = y.coerceIn(0, maxHeight - placeable.height)
    layout(maxWidth, maxHeight) {
        placeable.placeRelative(finalX, finalY)
    }
}

private const val DEGREES = (180f / Math.PI.toFloat())

