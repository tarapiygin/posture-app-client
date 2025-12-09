package com.example.postureapp.ui.indicators.front

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.postureapp.R
import com.example.postureapp.core.draw.drawAngleArc
import com.example.postureapp.core.draw.drawDottedPolyline
import com.example.postureapp.core.draw.drawLevelSegment
import com.example.postureapp.core.draw.positionAt
import com.example.postureapp.domain.landmarks.LandmarkSet
import com.example.postureapp.domain.metrics.ComputeFrontMetricsUseCase
import com.example.postureapp.domain.metrics.FrontMetrics
import com.example.postureapp.ui.indicators.BlueLevelAngleLabel
import com.example.postureapp.ui.indicators.IndicatorRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.postureapp.ui.indicators.computeBodyDeviationGeometry
import com.example.postureapp.ui.indicators.drawBodyAngleText
import com.example.postureapp.ui.indicators.drawBodyAxisLine
import com.example.postureapp.ui.indicators.drawBodyDeviationLine
import com.example.postureapp.ui.indicators.drawLevelGuideLine
import com.example.postureapp.ui.indicators.drawRedAngleLabel

private val YellowLine = Color(0xFFFFD60A)
private val GrayConnector = Color(0xFFCBCBD1)

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

            val parentWidthPx = canvasWidthPx
            val leftMarginPx = with(density) { 16.dp.toPx() }
            val levelLabelMap = metrics.levelAngles.associate { it.name to labelFor(it.name) }

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

                        // серые горизонтальные линии по уровням
                        metrics.levelAngles.forEach { level ->
                            drawLine(
                                color = GrayConnector.copy(alpha = 0.55f),
                                start = toCanvas(level.left),
                                end = toCanvas(level.right),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // соединяющая пунктирная линия между уровнями и яремной точкой
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

                        // базовая точка тела и яремная выемка в координатах Canvas
                        val base = toCanvas(metrics.bodyBase)
                        val jugular = toCanvas(metrics.jnPx)

                        // геометрия красной линии тела
                        val bodyGeometry = computeBodyDeviationGeometry(
                            base = base,
                            jugular = jugular
                        )

                        // зелёная вертикальная ось
                        drawBodyAxisLine(
                            baseX = bodyGeometry.base.x
                        )

                        // красная прерывистая линия тела
                        drawBodyDeviationLine(
                            geometry = bodyGeometry
                        )

                        // дуга угла между вертикалью и линией тела
                        val deviationVector = Offset(
                            x = bodyGeometry.jugular.x - bodyGeometry.base.x,
                            y = bodyGeometry.jugular.y - bodyGeometry.base.y
                        )
                        drawAngleArc(
                            center = bodyGeometry.base,
                            startVector = Offset(0f, -1f),
                            endVector = deviationVector,
                            color = IndicatorRed,
                            radius = 48.dp,
                            strokeWidth = 3.dp
                        )

                        // уровни: голубые горизонтальные линии, жёлтые сегменты и красные квадраты с углами
                        metrics.levelAngles.forEach { level ->
                            val left = toCanvas(level.left)
                            val right = toCanvas(level.right)
                            val y = (left.y + right.y) / 2f

                            // голубая линия уровня
                            drawLevelGuideLine(y = y)

                            // жёлтый сегмент между крайними точками уровня
                            drawLevelSegment(
                                start = left,
                                end = right,
                                color = YellowLine,
                                strokeWidth = 3.dp
                            )

                            // красный квадрат с величиной отклонения уровня и коннектором к красной линии
                            level.bodyDeviationDeg?.let { bodyDev ->
                                drawRedAngleLabel(
                                    y = y,
                                    angleDeg = bodyDev,
                                    geometry = bodyGeometry
                                )
                            }
                        }

                        // общий угол тела (красный текст рядом с базой)
                        drawBodyAngleText(
                            base = bodyGeometry.base,
                            angleDeg = metrics.bodyAngleDeg
                        )
                    }

                    // голубые квадраты с текстом для каждого уровня
                    metrics.levelAngles.forEach { level ->
                        val yCenter = (level.yPx / bitmap.height.toFloat()) * canvasHeightPx

                        BlueLevelAngleLabel(
                            text = "${levelLabelMap[level.name].orEmpty()}: ${formatDeg(level.deviationDeg)}",
                            modifier = Modifier.positionAt(
                                xPx = leftMarginPx,
                                yPx = yCenter,
                                parentWidth = parentWidthPx
                            )
                        )
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

private fun midpoint(a: Offset, b: Offset): Offset =
    Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)

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
