package com.example.postureapp.ui.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.postureapp.R
import com.example.postureapp.core.designsystem.components.CustomTitleTopBar
import com.example.postureapp.domain.landmarks.Landmark
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLandmarksScreen(
    resultId: String,
    imagePath: String,
    onBack: () -> Unit,
    onNavigateToIndicators: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditLandmarksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bitmap = rememberDecodedBitmap(imagePath)

    LaunchedEffect(resultId, imagePath) {
        viewModel.load(resultId, imagePath)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditLandmarksEvent.NavigateToIndicators -> onNavigateToIndicators(
                    event.resultId,
                    event.imagePath
                )
            }
        }
    }
    // ОБЕРНУЛИ Scaffold В Box, чтобы поверх него нарисовать оверлей
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CustomTitleTopBar(
                    title = stringResource(R.string.edit_title),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomActionBar(
                    canUndo = uiState.canUndo,
                    onUndo = viewModel::undo,
                    onFinalize = viewModel::finalizeEditing,
                    onReset = viewModel::reset
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    uiState.error -> ErrorState(onBack)
                    bitmap == null || uiState.isLoading || uiState.landmarkSet == null -> LoadingState()
                    else -> EditableContent(
                        bitmap = bitmap,
                        uiState = uiState,
                        onSelectPoint = { name, position -> viewModel.select(name, position) },
                        onStartDrag = viewModel::startDrag,
                        onDrag = viewModel::dragTo,
                        onDragEnd = viewModel::endDrag,
                        onTransform = viewModel::applyTransform,
                        onOpenHelp = viewModel::onOpenHelp,
                        onDismissHelp = viewModel::onDismissHelp
                    )
                }
            }
        }

        // ---------- ВСПЛЫВАЮЩИЙ ОВЕРЛЕЙ ПОВЕРХ ВСЕГО (включая bottomBar) ----------
        if (uiState.showHelpSheet && uiState.helpText.isNotBlank()) {
            // полупрозрачный фон по всей области
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        // тап по фону закрывает оверлей
                        detectTapGestures { viewModel.onDismissHelp() }
                    }
            ) {
                AnimatedVisibility(
                    visible = uiState.showHelpSheet,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight } // снизу вверх
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight } // вверх вниз
                    ) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                ) {
                    HelpSheet(
                        text = uiState.helpText,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        // -------------------------------------------------------------------------
    }
}

    @Composable

    private fun EditableContent(
        bitmap: Bitmap,
        uiState: EditLandmarksUiState,
        onSelectPoint: (String?, Offset?) -> Unit,
        onStartDrag: (String, Offset) -> Unit,
        onDrag: (Offset) -> Unit,
        onDragEnd: () -> Unit,
        onTransform: (Offset, Offset, Float, Size, Size) -> Unit,
        onOpenHelp: (String) -> Unit,
        onDismissHelp: () -> Unit
    ) {
        var mapper by remember { mutableStateOf<ImageSpaceMapper?>(null) }
        var renderedPoints by remember { mutableStateOf<List<RenderedPoint>>(emptyList()) }
        // флаг показа подсказки
        var showHint by remember { mutableStateOf(true) }

        val density = LocalDensity.current
        val baseHitRadiusPx = with(density) { 25.dp.toPx() }

        // Насколько выше пальца рисовать точку
        val fingerOffset = with(density) {
            Offset(
                x = (-30).dp.toPx(), // влево
                y = (-40).dp.toPx()  // вверх
            )
        }

        // ЕДИНЫЙ ОБРАБОТЧИК: tap + drag точки + pan + pinch-zoom
        val pointerModifier = Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    // 1. Ждём первое касание
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var activePointName: String? = null
                    var draggingPoint = false

                    // <<< НОВОЕ: при первом тапе скрываем подсказку >>>
                    if (showHint) {
                        showHint = false
                    }

                    // hit-тест по точкам
                    val hitRadius = baseHitRadiusPx / uiState.scale.coerceAtLeast(0.6f)
                    val hit = renderedPoints.lastOrNull { rendered ->
                        rendered.position.distanceTo(down.position) <= hitRadius
                    }

                    val currentMapperAtDown = mapper

                    if (hit != null && currentMapperAtDown != null) {
                        // Тап по точке → просто выделяем
                        activePointName = hit.landmark.point.name
                        onSelectPoint(activePointName, null)
                    } else {
                        // Тап по пустому месту → снимаем выделение
                        activePointName = null
                        onSelectPoint(null, null)
                    }

                    // Для pan/zoom
                    var previousCentroid = down.position
                    var previousSpan: Float? = null
                    var previousPointersCount = 1

                    // 2. Пока есть хотя бы один палец на экране
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val changes = event.changes

                        // Если все пальцы оторваны — завершаем жест
                        if (changes.all { !it.pressed }) {
                            if (draggingPoint) {
                                onDragEnd()
                                draggingPoint = false
                            }
                            break
                        }

                        val pressed = changes.filter { it.pressed }
                        if (pressed.isEmpty()) continue

                        val pressedCount = pressed.size
                        val positions = pressed.map { it.position }

                        // центр всех пальцев
                        val centroid = positions
                            .reduce { acc, offset -> acc + offset } / pressedCount.toFloat()

                        // "радиус" для pinch-zoom
                        val span: Float? =
                            if (pressedCount >= 2) {
                                val c = centroid
                                positions
                                    .map { (it - c).magnitude() }
                                    .average()
                                    .toFloat()
                            } else {
                                null
                            }

                        // === КЛЮЧЕВОЕ МЕСТО: смена числа пальцев ===
                        if (pressedCount != previousPointersCount) {
                            // Сбросить базу, чтобы не было "прыжка"
                            previousPointersCount = pressedCount
                            previousCentroid = centroid
                            previousSpan = span

                            // Просто съедаем событие и ждём следующее
                            changes.forEach { it.consume() }
                            continue
                        }

                        val pan = centroid - previousCentroid

                        // Коэффициент зума
                        val zoomFactor: Float =
                            if (previousSpan != null && span != null && previousSpan!! > 0f) {
                                val raw = span / previousSpan!!
                                val sensitivity = 2f
                                1f + (raw - 1f) * sensitivity
                            } else {
                                1f
                            }

                        val currentMapper = mapper

                        if (pressedCount >= 2) {
                            // ====== 2+ пальца → всегда pan + zoom ======
                            if (draggingPoint) {
                                onDragEnd()
                                draggingPoint = false
                                activePointName = null
                                onSelectPoint(null, null)
                            }

                            if (currentMapper != null) {
                                onTransform(
                                    centroid,
                                    pan,
                                    zoomFactor,
                                    currentMapper.viewportSize,
                                    currentMapper.contentSize
                                )
                            }
                        } else {
                            // ====== 1 палец ======
                            val fingerPos = positions[0]

                            if (activePointName != null) {
                                // --- перетаскивание точки ---
                                if (currentMapper != null) {
                                    val adjustedScreen = fingerPos + fingerOffset
                                    val imageOffset = currentMapper.screenToImage(adjustedScreen)

                                    if (!draggingPoint) {
                                        onStartDrag(activePointName!!, imageOffset)
                                        draggingPoint = true
                                    }
                                    onDrag(imageOffset)
                                }
                            } else {
                                // --- панорамирование одним пальцем ---
                                if (currentMapper != null && (pan.x != 0f || pan.y != 0f)) {
                                    onTransform(
                                        centroid,
                                        pan,
                                        1f,
                                        currentMapper.viewportSize,
                                        currentMapper.contentSize
                                    )
                                }
                            }
                        }

                        previousCentroid = centroid
                        if (span != null) previousSpan = span

                        changes.forEach { it.consume() }
                    }
                }
            }
        }



        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Слой с картинкой и точками — только здесь жесты
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(pointerModifier)
            ) {
                LandmarksCanvas(
                    bitmap = bitmap,
                    uiState = uiState,
                    onTransform = onTransform,
                    onMapperChanged = { mapper = it },
                    onRenderedPointsChanged = { renderedPoints = it }
                )

                if (showHint) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = stringResource(R.string.hint_aligned),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                val magnifierMapper = mapper
                if (uiState.showMagnifier && magnifierMapper != null) {
                    MagnifierOverlay(
                        source = bitmap,
                        centerImageSpace = uiState.magnifierCenter,
                        imageToScreen = { magnifierMapper.imageToScreen(it) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    )
                }
            }

            // Панель с названием точки и иконкой "!"
            uiState.activePoint?.let { point ->
                ReferencePanel(
                    point = point,
                    onHelp = { onOpenHelp(point.name) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }
    }

private const val LANDMARKS_TAG = "EditLandmarks"

@Composable
private fun LandmarksCanvas(
    bitmap: Bitmap,
    uiState: EditLandmarksUiState,
    onTransform: (Offset, Offset, Float, Size, Size) -> Unit,
    onMapperChanged: (ImageSpaceMapper?) -> Unit,
    onRenderedPointsChanged: (List<RenderedPoint>) -> Unit
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val density = LocalDensity.current
    val labelPaint = rememberLabelPaint()
    val accent = MaterialTheme.colorScheme.primary
    val labelOffset = with(density) { 8.dp.toPx() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val intrinsicSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
        val contentSize = calculateContentSize(viewportSize, intrinsicSize)
        if (contentSize.width <= 0f || contentSize.height <= 0f) {
            onMapperChanged(null)
            return@BoxWithConstraints
        }

        val mapper = remember(uiState.scale, uiState.offsetX, uiState.offsetY, viewportSize, contentSize) {
            ImageSpaceMapper(
                imageRect = computeImageRect(
                    viewportSize = viewportSize,
                    contentSize = contentSize,
                    scale = uiState.scale,
                    offsetX = uiState.offsetX,
                    offsetY = uiState.offsetY
                ),
                viewportSize = viewportSize,
                contentSize = contentSize
            )
        }
        LaunchedEffect(mapper) { onMapperChanged(mapper) }

        val renderedPoints = remember(uiState.allPoints, mapper) {
            uiState.allPoints.map { landmark ->
                RenderedPoint(
                    landmark = landmark,
                    position = mapper.imageToScreen(Offset(landmark.x, landmark.y))
                )
            }
        }
        SideEffect { onRenderedPointsChanged(renderedPoints) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val rect = mapper.imageRect
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
                dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt())
            )

            val sorted = uiState.allPoints.sortedBy {
                when {
                    it.point == uiState.activePoint -> 2
                    it.editable -> 1
                    else -> 0
                }
            }
            sorted.forEach { landmark ->
                val position = mapper.imageToScreen(Offset(landmark.x, landmark.y))
                drawLandmarkPoint(
                    position = position,
                    active = landmark.point == uiState.activePoint,
                    editable = landmark.editable,
                    accent = accent
                )
                drawContext.canvas.nativeCanvas.drawText(
                    landmark.code,
                    position.x + labelOffset,
                    position.y - labelOffset,
                    labelPaint
                )
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    canUndo: Boolean,
    onUndo: () -> Unit,
    onFinalize: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionChip(
            icon = Icons.Rounded.Undo,
            label = stringResource(R.string.btn_undo),
            enabled = canUndo,
            onClick = onUndo
        )
        ActionChip(
            icon = Icons.Rounded.PushPin,
            label = stringResource(R.string.btn_pin),
            onClick = onFinalize
        )
        ActionChip(
            icon = Icons.Rounded.RestartAlt,
            label = stringResource(R.string.btn_reset),
            onClick = onReset
        )
    }
}

@Composable
private fun RowScope.ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = if (enabled) 4.dp else 0.dp,
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = tint
            )
        }
    }
}

@Composable
private fun ErrorState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.edit_landmarks_error_missing),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.clickable(onClick = onBack)
        ) {
            Text(
                text = stringResource(R.string.action_back),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

@Composable
private fun rememberLabelPaint(): Paint {
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.onSurface
    return remember(color, density) {
        Paint().apply {
            this.color = color.toArgb()
            textSize = with(density) { 11.sp.toPx() }
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
}

private fun Offset.magnitude(): Float = sqrt(x * x + y * y)

private fun Offset.distanceTo(other: Offset): Float = (this - other).magnitude()

@Composable
private fun HelpSheet(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(24.dp)
        )
    }
}

private data class RenderedPoint(
    val landmark: Landmark,
    val position: Offset
)

private data class ImageSpaceMapper(
    val imageRect: Rect,
    val viewportSize: Size,
    val contentSize: Size
) {
    fun imageToScreen(image: Offset): Offset {
        return Offset(
            x = imageRect.left + image.x.coerceIn(0f, 1f) * imageRect.width,
            y = imageRect.top + image.y.coerceIn(0f, 1f) * imageRect.height
        )
    }

    fun screenToImage(screen: Offset): Offset {
        val x = if (imageRect.width == 0f) 0f else (screen.x - imageRect.left) / imageRect.width
        val y = if (imageRect.height == 0f) 0f else (screen.y - imageRect.top) / imageRect.height
        return Offset(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
    }
}

private fun calculateContentSize(viewport: Size, intrinsic: Size): Size {
    if (intrinsic.width <= 0f || intrinsic.height <= 0f) return Size.Zero
    val scale = min(viewport.width / intrinsic.width, viewport.height / intrinsic.height)
    val width = intrinsic.width * scale
    val height = intrinsic.height * scale
    return Size(width, height)
}

private fun computeImageRect(
    viewportSize: Size,
    contentSize: Size,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Rect {
    val scaledWidth = contentSize.width * scale
    val scaledHeight = contentSize.height * scale
    val left = (viewportSize.width - scaledWidth) / 2f + offsetX
    val top = (viewportSize.height - scaledHeight) / 2f + offsetY
    return Rect(
        left,
        top,
        left + scaledWidth,
        top + scaledHeight
    )
}
