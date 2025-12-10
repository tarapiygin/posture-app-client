package com.example.postureapp.ui.indicators.right

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.postureapp.R
import com.example.postureapp.core.draw.drawAngleArc
import com.example.postureapp.core.draw.positionAt
import com.example.postureapp.domain.landmarks.LandmarkSet
import com.example.postureapp.domain.metrics.right.ComputeRightMetricsUseCase
import com.example.postureapp.domain.metrics.right.RightMetrics
import com.example.postureapp.ui.indicators.BlueLevelAngleLabel
import com.example.postureapp.ui.indicators.IndicatorRed
import com.example.postureapp.ui.indicators.computeBodyDeviationGeometry
import com.example.postureapp.ui.indicators.drawBodyAngleText
import com.example.postureapp.ui.indicators.drawBodyAxisLine
import com.example.postureapp.ui.indicators.drawBodyDeviationLine
import com.example.postureapp.ui.indicators.drawLevelGuideLine
import com.example.postureapp.ui.indicators.drawRedAngleLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ChainColor = Color.White.copy(alpha = 0.7f)

@Composable
fun RightIndicatorsPanel(
    imagePath: String,
    landmarksFinal: LandmarkSet,
    onResetToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberDecodedBitmap(imagePath)
    val metrics = remember(landmarksFinal) {
        ComputeRightMetricsUseCase().invoke(landmarksFinal)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            bitmap == null || metrics == null -> LoadingState(modifier = Modifier.fillMaxSize())
            else -> RightIndicatorsContent(
                bitmap = bitmap,
                metrics = metrics,
                landmarks = landmarksFinal,
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
private fun RightIndicatorsContent(
    bitmap: Bitmap,
    metrics: RightMetrics,
    landmarks: LandmarkSet? = null,
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
            val refWidth = landmarks?.imageWidth?.takeIf { it > 0 }?.toFloat() ?: bitmap.width.toFloat()
            val refHeight = landmarks?.imageHeight?.takeIf { it > 0 }?.toFloat() ?: bitmap.height.toFloat()

            val drawnWidth = canvasHeightPx * (refWidth / refHeight)
            val leftOffset = (canvasWidthPx - drawnWidth) / 2f
            val scaleX = drawnWidth / refWidth
            val scaleY = canvasHeightPx / refHeight

            fun toCanvas(offset: Offset): Offset =
                Offset(leftOffset + offset.x * scaleX, offset.y * scaleY)

            val ankleCanvas = toCanvas(metrics.greenBase)
            val earCanvas = toCanvas(metrics.earPx)
            val chainCanvas = metrics.chainPoints.map { toCanvas(it) }

            val leftMarginPx = with(density) { 16.dp.toPx() }

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
                    val ankle = ankleCanvas
                    val ear = earCanvas

                    // Геометрия красной линии (от голеностопа к уху)
                    val bodyGeometry = computeBodyDeviationGeometry(
                        base = ankle,
                        jugular = ear
                    )

                    // Зелёная вертикальная ось через голеностоп (baseX)
                    drawBodyAxisLine(
                        baseX = bodyGeometry.base.x
                    )

                    // Красная прерывистая линия тела (по нашим виджетам)
                    drawBodyDeviationLine(
                        geometry = bodyGeometry
                    )

                    // Дуга угла между вертикалью и линией тела (body angle)
                    val deviationVector = ear - ankle
                    drawAngleArc(
                        center = ankle,
                        startVector = Offset(0f, -1f),
                        endVector = deviationVector,
                        color = IndicatorRed,
                        radius = 48.dp,
                        strokeWidth = 3.dp
                    )

                    // Подпись общего угла тела около голеностопа (красный текст)
                    drawBodyAngleText(
                        base = ankle,
                        angleDeg = metrics.bodyAngleDeg
                    )

                    // Голубая горизонтальная линия через ухо (CVA reference)
                    drawLevelGuideLine(
                        y = ear.y
                    )

                    // Красные квадраты-метки углов вдоль цепи (заменяем старые DegreePill)
                    metrics.segments.forEach { segment ->
                        val anchorCanvas = toCanvas(segment.anchorPx)
                        drawRedAngleLabel(
                            y = anchorCanvas.y,
                            angleDeg = segment.angleDeg,
                            geometry = bodyGeometry
                        )
                    }

                    // Линия миофасциальной / кинематической цепи
                    if (chainCanvas.size >= 2) {
                        val path = Path().apply {
                            moveTo(chainCanvas.first().x, chainCanvas.first().y)
                            chainCanvas.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = path,
                            color = ChainColor,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                // Подпись CVA в виде голубого квадрата
                BlueLevelAngleLabel(
                    text = "CVA: ${formatDeg(metrics.cvaDeg)}",
                    modifier = Modifier.positionAt(
                        xPx = leftMarginPx,
                        yPx = earCanvas.y,
                        parentWidth = canvasWidthPx
                    )
                )
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

    LaunchedEffect(path) {
        val decoded = withContext(Dispatchers.IO) {
            if (path.isBlank()) null else BitmapFactory.decodeFile(path)
        }
        if (decoded !== bitmap) {
            bitmap?.recycle()
            bitmap = decoded
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

private operator fun Offset.minus(other: Offset): Offset = Offset(x - other.x, y - other.y)

private fun formatDeg(value: Float): String {
    val v = if (value < 0.1f) 0f else value
    return String.format("%.1f\u00B0", v)
}
