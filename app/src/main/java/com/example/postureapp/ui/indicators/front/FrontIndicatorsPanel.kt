package com.example.postureapp.ui.indicators.front

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.postureapp.R
import com.example.postureapp.core.draw.DegreePill
import com.example.postureapp.core.draw.LevelLabelChip
import com.example.postureapp.core.draw.drawAngleArc
import com.example.postureapp.core.draw.drawDottedLine
import com.example.postureapp.core.draw.drawDottedPolyline
import com.example.postureapp.core.draw.drawDottedVertical
import com.example.postureapp.core.draw.drawLevelGuide
import com.example.postureapp.core.draw.drawLevelSegment
import com.example.postureapp.domain.landmarks.LandmarkSet
import com.example.postureapp.domain.metrics.ComputeFrontMetricsUseCase
import com.example.postureapp.domain.metrics.FrontMetrics
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val GreenDotted = Color(0xFF2ECC71)
private val RedDotted = Color(0xFFFF3B30)
private val YellowLine = Color(0xFFFFD60A)
private val BlueGuide = Color(0xFF89B4F8)
private val GrayConnector = Color(0xFFCBCBD1)
private val BodyPillColor = RedDotted

@Composable
fun FrontIndicatorsPanel(
    imagePath: String,
    landmarksFinal: LandmarkSet,
    onResetToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberDecodedBitmap(imagePath)
    val metrics = remember(landmarksFinal) {
        ComputeFrontMetricsUseCase().invoke(landmarksFinal)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            bitmap == null || metrics == null -> LoadingState(modifier = Modifier.fillMaxSize())
            else -> FrontIndicatorsContent(
                bitmap = bitmap,
                metrics = metrics,
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            onClick = onResetToEdit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reset_report),
                contentDescription = stringResource(R.string.action_reset_report),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FrontIndicatorsContent(
    bitmap: Bitmap,
    metrics: FrontMetrics,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()
            val canvasWidthDp = with(density) { canvasWidthPx.toDp() }
            val canvasHeightDp = with(density) { canvasHeightPx.toDp() }

            val labelMarginPx = with(density) { 12.dp.toPx() }
            val labelOffsetPx = with(density) { 14.dp.toPx() }
            val parentWidthPx = canvasWidthPx
            val leftMarginPx = with(density) { 16.dp.toPx() }

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(canvasWidthDp, canvasHeightDp)
                        .align(Alignment.TopCenter)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier.matchParentSize()
                    )

                    Canvas(modifier = Modifier.matchParentSize()) {
                        val drawnWidth = size.height * (bitmap.width.toFloat() / bitmap.height.toFloat())
                        val leftOffset = (size.width - drawnWidth) / 2f
                        val scaleX = drawnWidth / bitmap.width.toFloat()
                        val scaleY = size.height / bitmap.height.toFloat()
                        fun toCanvas(offset: Offset): Offset =
                            Offset(leftOffset + offset.x * scaleX, offset.y * scaleY)

                        metrics.levelAngles.forEach { level ->
                            drawLine(
                                color = GrayConnector.copy(alpha = 0.55f),
                                start = toCanvas(level.left),
                                end = toCanvas(level.right),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        val midPoints = buildList {
                            val lookup = metrics.levelAngles.associateBy { it.name }
                            lookup["Feet"]?.let { add(midpoint(toCanvas(it.left), toCanvas(it.right))) }
                            lookup["Knees"]?.let { add(midpoint(toCanvas(it.left), toCanvas(it.right))) }
                            lookup["ASIS"]?.let { add(midpoint(toCanvas(it.left), toCanvas(it.right))) }
                            lookup["Shoulders"]?.let { add(midpoint(toCanvas(it.left), toCanvas(it.right))) }
                            add(toCanvas(metrics.jnPx))
                        }
                        if (midPoints.size >= 2) {
                            drawDottedPolyline(
                                points = midPoints,
                                color = GrayConnector.copy(alpha = 0.45f),
                                strokeWidth = 2.dp,
                                dashOn = 6.dp,
                                dashOff = 6.dp
                            )
                        }

                        val base = toCanvas(metrics.bodyBase)
                        val jugular = toCanvas(metrics.jnPx)
                        drawDottedVertical(
                            x = base.x,
                            color = GreenDotted,
                            strokeWidth = 3.dp
                        )

                        val deviationVector = jugular - base
                        val deviationLength = deviationVector.length().takeIf { it > 0.0001f } ?: 1f
                        val dir = deviationVector / deviationLength
                        val factor = if (dir.y == 0f) 1f else base.y / -dir.y
                        val extended = base + dir * factor

                        drawDottedLine(
                            start = base,
                            end = extended,
                            color = RedDotted,
                            strokeWidth = 3.dp
                        )

                        drawAngleArc(
                            center = base,
                            startVector = Offset(0f, -1f),
                            endVector = deviationVector,
                            color = RedDotted,
                            radius = 48.dp,
                            strokeWidth = 3.dp
                        )

                        metrics.levelAngles.forEach { level ->
                            val left = toCanvas(level.left)
                            val right = toCanvas(level.right)
                            val y = (left.y + right.y) / 2f
                            drawLevelGuide(
                                y = y,
                                color = BlueGuide,
                                strokeWidth = 2.dp
                            )
                            drawLevelSegment(
                                start = left,
                                end = right,
                                color = YellowLine,
                                strokeWidth = 6.dp
                            )
                        }

                        val text = "${metrics.bodyAngleDeg.roundToInt()}\u00B0"
                        val paint = android.graphics.Paint().apply {
                            color = RedDotted.toArgb()
                            textSize = 14.sp.toPx()
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.DEFAULT,
                                android.graphics.Typeface.BOLD
                            )
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            text,
                            base.x + 12.dp.toPx(),
                            base.y - 12.dp.toPx(),
                            paint
                        )
                    }

                    metrics.levelAngles.forEach { level ->
                        val label = labelFor(level.name)
                        val yCenter = (level.yPx / bitmap.height.toFloat()) * canvasHeightPx

                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.positionAt(
                                xPx = leftMarginPx,
                                yPx = yCenter,
                                parentWidth = parentWidthPx
                            )
                        ) {
                            LevelLabelChip(text = label)
                            DegreePill(text = formatDeg(level.deviationDeg))
                        }

                        level.bodyDeviationDeg?.let { bodyDev ->
                            Text(
                                text = formatDeg(bodyDev),
                                color = BodyPillColor,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.positionAt(
                                    xPx = canvasWidthPx / 2f,
                                    yPx = yCenter,
                                    parentWidth = canvasWidthPx
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun rememberDecodedBitmap(path: String): Bitmap? {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(path) {
        scope.launch {
            val decoded = withContext(Dispatchers.IO) {
                if (path.isBlank()) null else BitmapFactory.decodeFile(path)
            }
            if (decoded !== bitmap) {
                bitmap?.recycle()
                bitmap = decoded
            }
        }
    }

    DisposableEffect(path) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }
    return bitmap
}

private fun midpoint(a: Offset, b: Offset): Offset = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)

private fun Offset.length(): Float = sqrt(x * x + y * y)

private operator fun Offset.div(scalar: Float): Offset = Offset(x / scalar, y / scalar)

private operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)

private operator fun Offset.minus(other: Offset): Offset = Offset(x - other.x, y - other.y)

private operator fun Offset.times(scale: Float): Offset = Offset(x * scale, y * scale)

private fun Modifier.positionAt(
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

private fun formatDeg(value: Float): String {
    val v = if (value < 0.1f) 0f else value
    return String.format("%.1f\u00B0", v)
}

@Composable
private fun labelFor(name: String): String = when (name) {
    "Ears" -> stringResource(R.string.lbl_ears)
    "Shoulders" -> stringResource(R.string.lbl_shoulders)
    "ASIS" -> stringResource(R.string.lbl_asis)
    "Knees" -> stringResource(R.string.lbl_knees)
    "Feet" -> stringResource(R.string.lbl_feet)
    else -> name
}
