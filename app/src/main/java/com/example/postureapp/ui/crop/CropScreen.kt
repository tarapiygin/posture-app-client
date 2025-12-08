package com.example.postureapp.ui.crop

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.example.postureapp.R
import com.example.postureapp.core.navigation.decodePath
import com.example.postureapp.core.storage.ImageSaver
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    encodedPath: String,
    rotationDegrees: Int,
    onBack: () -> Unit,
    onCropped: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val originalPath = remember(encodedPath) { decodePath(encodedPath).orEmpty() }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(originalPath, rotationDegrees) {
        val loadedBitmap = withContext(Dispatchers.IO) {
            val source = BitmapFactory.decodeFile(originalPath) ?: return@withContext null

            val exifRotation = runCatching {
                val exif = ExifInterface(originalPath)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }.getOrDefault(0)

            val totalRotation = if (exifRotation != 0) exifRotation else rotationDegrees

            if (totalRotation != 0) {
                val matrix = Matrix().apply { postRotate(totalRotation.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    source,
                    0,
                    0,
                    source.width,
                    source.height,
                    matrix,
                    true
                )
                if (rotated != source) source.recycle()
                rotated
            } else {
                source
            }
        }

        if (loadedBitmap == null) {
            File(originalPath).delete()
            onBack()
        } else {
            bitmap = loadedBitmap
        }
    }

    if (bitmap == null) {
        LoadingCropScreen(onBack = {
            File(originalPath).delete()
            onBack()
        })
        return
    }

    CropContent(
        bitmap = bitmap!!,
        isSaving = isSaving,
        onBack = {
            File(originalPath).delete()
            onBack()
        },
        onConfirm = { cropRect ->
            scope.launch {
                isSaving = true
                val outputFile = withContext(Dispatchers.IO) {
                    val cropped = cropBitmap(bitmap!!, cropRect)
                    val file = ImageSaver.createCaptureFile(context)
                    FileOutputStream(file).use { out ->
                        cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    File(originalPath).delete()
                    file
                }
                isSaving = false
                onCropped(outputFile.absolutePath)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingCropScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.crop_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.crop_loading),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropContent(
    bitmap: Bitmap,
    isSaving: Boolean,
    onBack: () -> Unit,
    onConfirm: (CropRect) -> Unit
) {
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }

    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()

    var cropRect by remember {
        mutableStateOf(
            CropRect(
                left = 0.1f,
                top = 0.1f,
                right = 0.9f,
                bottom = 0.9f
            )
        )
    }

    val minCropSize = 0.1f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.crop_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        enabled = !isSaving,
                        onClick = { onConfirm(cropRect) }
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
                .onGloballyPositioned {
                    containerWidth = it.size.width
                    containerHeight = it.size.height
                }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            if (containerWidth > 0 && containerHeight > 0) {
                CropOverlay(
                    cropRect = cropRect,
                    containerWidth = containerWidth,
                    containerHeight = containerHeight,
                    imageAspect = imageAspect,
                    onUpdateRect = { cropRect = it },
                    minCropSize = minCropSize
                )
            }
        }
    }
}

private enum class DragMode {
    NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

@Composable
private fun CropOverlay(
    cropRect: CropRect,
    containerWidth: Int,
    containerHeight: Int,
    imageAspect: Float,
    onUpdateRect: (CropRect) -> Unit,
    minCropSize: Float
) {
    val containerAspect = containerWidth.toFloat() / containerHeight.toFloat()
    val displayedWidth: Float
    val displayedHeight: Float

    if (imageAspect > containerAspect) {
        displayedWidth = containerWidth.toFloat()
        displayedHeight = displayedWidth / imageAspect
    } else {
        displayedHeight = containerHeight.toFloat()
        displayedWidth = displayedHeight * imageAspect
    }

    val offsetX = (containerWidth - displayedWidth) / 2f
    val offsetY = (containerHeight - displayedHeight) / 2f

    val density = androidx.compose.ui.platform.LocalDensity.current

    // Визуальный размер ручки
    val handleVisualSize = 24.dp
    // Фактическая hit-зона для пальца (увеличенная)
    val handleHitSize = 40.dp

    val handleSizePx = with(density) { handleVisualSize.toPx() }
    val handleHitSizePx = with(density) { handleHitSize.toPx() }

    // всегда актуальный rect внутри жестов
    val currentRect by rememberUpdatedState(cropRect)

    var dragMode by remember { mutableStateOf(DragMode.NONE) }

    fun clampRect(rect: CropRect): CropRect {
        return rect.copy(
            left = rect.left.coerceIn(0f, 1f),
            top = rect.top.coerceIn(0f, 1f),
            right = rect.right.coerceIn(0f, 1f),
            bottom = rect.bottom.coerceIn(0f, 1f)
        )
    }

    fun updateRect(change: (CropRect) -> CropRect) {
        onUpdateRect(clampRect(change(currentRect)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // PINCH-ZOOM (двумя пальцами): масштабируем рамку вокруг центра жеста
            .pointerInput(Unit) {
                detectTransformGestures { centroid, _, zoom, _ ->
                    if (zoom == 1f) return@detectTransformGestures

                    updateRect { r ->
                        val cxNorm = ((centroid.x - offsetX) / displayedWidth)
                        val cyNorm = ((centroid.y - offsetY) / displayedHeight)

                        if (!cxNorm.isFinite() || !cyNorm.isFinite()) return@updateRect r

                        val scale = zoom
                        var left = cxNorm + (r.left - cxNorm) * scale
                        var right = cxNorm + (r.right - cxNorm) * scale
                        var top = cyNorm + (r.top - cyNorm) * scale
                        var bottom = cyNorm + (r.bottom - cyNorm) * scale

                        var width = right - left
                        var height = bottom - top

                        if (width < minCropSize) {
                            val diff = (minCropSize - width) / 2f
                            left -= diff
                            right += diff
                            width = right - left
                        }
                        if (height < minCropSize) {
                            val diff = (minCropSize - height) / 2f
                            top -= diff
                            bottom += diff
                            height = bottom - top
                        }

                        r.copy(
                            left = left,
                            top = top,
                            right = right,
                            bottom = bottom
                        )
                    }
                }
            }
            // ОДИН ПАЛЕЦ: перемещение рамки и углов
            .pointerInput(Unit) {
                awaitEachGesture {
                    // 1. Ждем первого касания
                    val down = awaitFirstDown(requireUnconsumed = false)

                    val rect = currentRect
                    val left = offsetX + rect.left * displayedWidth
                    val top = offsetY + rect.top * displayedHeight
                    val right = offsetX + rect.right * displayedWidth
                    val bottom = offsetY + rect.bottom * displayedHeight

                    val halfHit = handleHitSizePx / 2f

                    fun inRect(
                        x: Float,
                        y: Float,
                        l: Float,
                        t: Float,
                        r: Float,
                        b: Float
                    ): Boolean = x in l..r && y in t..b

                    val startX = down.position.x
                    val startY = down.position.y

                    val inTopLeft = inRect(
                        startX,
                        startY,
                        left - halfHit,
                        top - halfHit,
                        left + halfHit,
                        top + halfHit
                    )
                    val inBottomRight = inRect(
                        startX,
                        startY,
                        right - halfHit,
                        bottom - halfHit,
                        right + halfHit,
                        bottom + halfHit
                    )
                    val inBottomLeft = inRect(
                        startX,
                        startY,
                        left - halfHit,
                        bottom - halfHit,
                        left + halfHit,
                        bottom + halfHit
                    )
                    val inTopRight = inRect(
                        startX,
                        startY,
                        right - halfHit,
                        top - halfHit,
                        right + halfHit,
                        top + halfHit
                    )

                    dragMode = when {
                        inTopLeft -> DragMode.TOP_LEFT
                        inTopRight -> DragMode.TOP_RIGHT
                        inBottomLeft -> DragMode.BOTTOM_LEFT
                        inBottomRight -> DragMode.BOTTOM_RIGHT
                        inRect(startX, startY, left, top, right, bottom) -> DragMode.MOVE
                        else -> DragMode.NONE
                    }

                    var pointerId = down.id
                    var lastPos = down.position

                    // 2. Тянем, пока палец зажат
                    while (true) {
                        val event = awaitPointerEvent()

                        // Если подключился второй палец — выходим, даём работать detectTransformGestures
                        if (event.changes.size > 1) {
                            dragMode = DragMode.NONE
                            break
                        }

                        val change = event.changes.firstOrNull { it.id == pointerId } ?: continue

                        if (!change.pressed) {
                            // палец отпустили
                            break
                        }

                        val delta = change.position - lastPos
                        lastPos = change.position

                        if (dragMode != DragMode.NONE) {
                            val dxNorm = delta.x / displayedWidth
                            val dyNorm = delta.y / displayedHeight

                            when (dragMode) {
                                DragMode.MOVE -> {
                                    updateRect {
                                        val width = it.right - it.left
                                        val height = it.bottom - it.top
                                        var newLeft = it.left + dxNorm
                                        var newTop = it.top + dyNorm
                                        newLeft = newLeft.coerceIn(0f, 1f - width)
                                        newTop = newTop.coerceIn(0f, 1f - height)
                                        it.copy(
                                            left = newLeft,
                                            top = newTop,
                                            right = newLeft + width,
                                            bottom = newTop + height
                                        )
                                    }
                                }

                                DragMode.TOP_LEFT -> {
                                    updateRect {
                                        var newLeft = it.left + dxNorm
                                        var newTop = it.top + dyNorm
                                        val maxLeft = it.right - minCropSize
                                        val maxTop = it.bottom - minCropSize
                                        newLeft = newLeft.coerceIn(0f, maxLeft)
                                        newTop = newTop.coerceIn(0f, maxTop)
                                        it.copy(left = newLeft, top = newTop)
                                    }
                                }

                                DragMode.TOP_RIGHT -> {
                                    updateRect {
                                        var newRight = it.right + dxNorm
                                        var newTop = it.top + dyNorm
                                        val minRight = it.left + minCropSize
                                        val maxTop = it.bottom - minCropSize
                                        newRight = newRight.coerceIn(minRight, 1f)
                                        newTop = newTop.coerceIn(0f, maxTop)
                                        it.copy(right = newRight, top = newTop)
                                    }
                                }

                                DragMode.BOTTOM_LEFT -> {
                                    updateRect {
                                        var newLeft = it.left + dxNorm
                                        var newBottom = it.bottom + dyNorm
                                        val maxLeft = it.right - minCropSize
                                        val minBottom = it.top + minCropSize
                                        newLeft = newLeft.coerceIn(0f, maxLeft)
                                        newBottom = newBottom.coerceIn(minBottom, 1f)
                                        it.copy(left = newLeft, bottom = newBottom)
                                    }
                                }

                                DragMode.BOTTOM_RIGHT -> {
                                    updateRect {
                                        var newRight = it.right + dxNorm
                                        var newBottom = it.bottom + dyNorm
                                        val minRight = it.left + minCropSize
                                        val minBottom = it.top + minCropSize
                                        newRight = newRight.coerceIn(minRight, 1f)
                                        newBottom = newBottom.coerceIn(minBottom, 1f)
                                        it.copy(right = newRight, bottom = newBottom)
                                    }
                                }

                                DragMode.NONE -> Unit
                            }

                            change.consume()
                        }
                    }

                    dragMode = DragMode.NONE
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = offsetX + cropRect.left * displayedWidth
            val top = offsetY + cropRect.top * displayedHeight
            val right = offsetX + cropRect.right * displayedWidth
            val bottom = offsetY + cropRect.bottom * displayedHeight

            val full = Path().apply {
                addRect(
                    androidx.compose.ui.geometry.Rect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height
                    )
                )
            }

            val cutout = Path().apply {
                addRect(
                    androidx.compose.ui.geometry.Rect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom
                    )
                )
            }

            val overlay = Path().apply {
                op(full, cutout, PathOperation.Difference)
            }

            drawPath(
                path = overlay,
                color = Color.Black.copy(alpha = 0.55f)
            )

            drawRect(
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                color = Color.White,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // визуальные ручки (используем визуальный размер)
        CropHandle(
            x = offsetX + cropRect.left * displayedWidth,
            y = offsetY + cropRect.top * displayedHeight,
            handleSizePx = handleSizePx
        )
        CropHandle(
            x = offsetX + cropRect.right * displayedWidth,
            y = offsetY + cropRect.bottom * displayedHeight,
            handleSizePx = handleSizePx
        )
        CropHandle(
            x = offsetX + cropRect.left * displayedWidth,
            y = offsetY + cropRect.bottom * displayedHeight,
            handleSizePx = handleSizePx
        )
        CropHandle(
            x = offsetX + cropRect.right * displayedWidth,
            y = offsetY + cropRect.top * displayedHeight,
            handleSizePx = handleSizePx
        )
    }
}

@Composable
private fun CropHandle(
    x: Float,
    y: Float,
    handleSizePx: Float
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val handleSize = with(density) { handleSizePx.toDp() }
    val offsetXDp = with(density) { (x - handleSizePx / 2f).toDp() }
    val offsetYDp = with(density) { (y - handleSizePx / 2f).toDp() }

    Box(
        modifier = Modifier
            .offset(x = offsetXDp, y = offsetYDp)
            .size(handleSize)
            .background(Color.White, shape = MaterialTheme.shapes.small)
    )
}

private fun cropBitmap(bitmap: Bitmap, cropRect: CropRect): Bitmap {
    val left = (cropRect.left * bitmap.width)
        .roundToInt()
        .coerceIn(0, bitmap.width - 1)
    val right = (cropRect.right * bitmap.width)
        .roundToInt()
        .coerceIn(left + 1, bitmap.width)
    val top = (cropRect.top * bitmap.height)
        .roundToInt()
        .coerceIn(0, bitmap.height - 1)
    val bottom = (cropRect.bottom * bitmap.height)
        .roundToInt()
        .coerceIn(top + 1, bitmap.height)

    return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
}
