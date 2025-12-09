package com.example.postureapp.ui.edit

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.core.graphics.ImageTransform
import com.example.postureapp.core.report.Side
import com.example.postureapp.data.processing.ProcessingStore
import com.example.postureapp.domain.landmarks.AnatomicalPoint
import com.example.postureapp.domain.landmarks.Landmark
import com.example.postureapp.domain.landmarks.LandmarkSet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayDeque
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditLandmarksViewModel @Inject constructor(
    private val processingStore: ProcessingStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val dispatcher = Dispatchers.Default
    private val undoStack = ArrayDeque<LandmarkSet>()
    /* хранит исходник для кнопок Reset/Undo; undoStack позволяет откатить изменения.*/
    private var autoSetFull: LandmarkSet? = null
    private var workingSet: LandmarkSet? = null
    private var pendingSnapshot: LandmarkSet? = null
    private var currentSide: Side = Side.FRONT

    private val _uiState = MutableStateFlow(EditLandmarksUiState())
    val uiState: StateFlow<EditLandmarksUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditLandmarksEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun load(resultId: String, imagePath: String, side: Side) {
        /*достаёт автогенерированные точки из
        ProcessingStore, добавляет синтетические точки (LandmarkSet.recomputeSynthetic())
        и кладёт их в uiState*/
        val cached = _uiState.value
        if (cached.resultId == resultId && cached.landmarkSet != null && cached.side == side) return
        currentSide = side

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = false,
                    imagePath = imagePath,
                    side = side
                )
            }
            val source = withContext(dispatcher) {
                processingStore.getFinal(resultId)
                    ?: processingStore.getAuto(resultId)
                    ?: processingStore.get(resultId)
            }
            if (source == null) {
                _uiState.update {
                    it.copy(
                        resultId = resultId,
                        imagePath = imagePath,
                        landmarkSet = null,
                        isLoading = false,
                        error = true
                    )
                }
                return@launch
            }

            val normalized = withContext(dispatcher) {
                when (side) {
                    Side.FRONT -> source.toFrontSide()
                    Side.RIGHT -> source.toRightSide()
                }
            }
            val visible = normalized
            Log.d("EditLandmarks", "points = ${visible.points.map { it.point to (it.x to it.y) }}")
            autoSetFull = normalized
            workingSet = normalized
            undoStack.clear()
            pendingSnapshot = null

            _uiState.value = EditLandmarksUiState(
                resultId = resultId,
                imagePath = imagePath,
                landmarkSet = visible,
                isLoading = false,
                error = false,
                canUndo = false,
                side = side
            )
        }
    }

    fun select(name: String?, initialPosition: Offset? = null) {
//        println("select name=$name initialPosition=$initialPosition")
        if (name == null) {
            this.clearSelection()
            return
        }
        val point = runCatching { AnatomicalPoint.valueOf(name) }.getOrNull() ?: return
        if (initialPosition != null) {
            startDrag(point.name, initialPosition)
        } else {
            _uiState.update {
                it.copy(
                    activePoint = point,
                    showHelpSheet = it.showHelpSheet
                )
            }
        }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(activePoint = null, showMagnifier = false) }
    }

    fun startDrag(name: String, imagePosition: Offset) {
        val point = runCatching { AnatomicalPoint.valueOf(name) }.getOrNull() ?: return
        val current = workingSet ?: return
        pendingSnapshot = current
        _uiState.update {
            it.copy(
                activePoint = point,
                showMagnifier = true,
                magnifierCenter = imagePosition
            )
        }
    }

    fun dragTo(imagePosition: Offset) {
        val point = _uiState.value.activePoint ?: return
        val current = workingSet ?: return
        val updated = current.withUpdated(point.name, imagePosition.x, imagePosition.y)
        if (updated == current) return
        workingSet = updated
        val visible = updated
        _uiState.update {
            it.copy(
                landmarkSet = visible,
                magnifierCenter = imagePosition
            )
        }
    }

    fun endDrag() {
        val snapshot = pendingSnapshot
        val current = workingSet
        if (snapshot != null && current != null && snapshot != current) {
            undoStack.addLast(snapshot)
        }
        pendingSnapshot = null
        _uiState.update {
            it.copy(
                showMagnifier = false,
                canUndo = undoStack.isNotEmpty()
            )
        }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        workingSet = previous
        val visible = previous
        _uiState.update {
            it.copy(
                landmarkSet = visible,
                canUndo = undoStack.isNotEmpty()
            )
        }
    }

    fun reset() {
        val base = autoSetFull ?: return
        val current = workingSet ?: return
        if (current == base) return
        undoStack.addLast(current)
        workingSet = base
        val visible = base
        _uiState.update {
            it.copy(
                landmarkSet = visible,
                canUndo = undoStack.isNotEmpty()
            )
        }
    }

    fun applyTransform(
        centroid: Offset,
        panChange: Offset,
        zoomChange: Float,
        viewportSize: Size,
        contentSize: Size
    ) {
        _uiState.update { state ->
            val transform = InteractiveTransform(
                scale = state.scale,
                tx = state.offsetX,
                ty = state.offsetY,
                viewport = viewportSize,
                content = contentSize
            )

            transform.apply(
                centroid = centroid,
                pan = panChange,
                zoom = zoomChange
            )

            state.copy(
                scale = transform.scale,
                offsetX = transform.tx,
                offsetY = transform.ty
            )
        }
    }

    fun finalizeEditing() {
        val current = workingSet ?: return
        val resultId = _uiState.value.resultId
        if (resultId.isBlank()) return
        viewModelScope.launch(dispatcher) {
            processingStore.putFinal(resultId, current)
            _events.send(EditLandmarksEvent.NavigateToIndicators(resultId, _uiState.value.imagePath))
        }
    }

    fun onOpenHelp(name: String) {
        val point = runCatching { AnatomicalPoint.valueOf(name) }.getOrNull() ?: return
        val text = context.getString(point.helpTextRes)
        _uiState.update {
            it.copy(
                showHelpSheet = true,
                helpText = text
            )
        }
    }

    fun onDismissHelp() {
        _uiState.update { it.copy(showHelpSheet = false) }
    }

    private fun LandmarkSet.mergeWith(override: LandmarkSet): LandmarkSet {
        val overrideMap = override.points.associateBy { it.point }
        val merged = points.map { base ->
            overrideMap[base.point]?.let { over ->
                base.copy(
                    x = over.x,
                    y = over.y,
                    z = over.z,
                    visibility = over.visibility,
                    editable = over.editable,
                    code = over.code
                )
            } ?: base
        }.toMutableList()

        override.points.forEach { over ->
            if (merged.none { it.point == over.point }) {
                merged.add(over)
            }
        }
        return copy(points = merged)
    }

    private data class InteractiveTransform(
        var scale: Float,
        var tx: Float,
        var ty: Float,
        val viewport: Size,
        val content: Size
    ) {
        fun apply(centroid: Offset, pan: Offset, zoom: Float) {
            val minScale = 1f
            val maxScale = 5f

            val currentTransform = ImageTransform(scale, tx, ty)
            currentTransform.updateBounds(
                viewportWidth = viewport.width,
                viewportHeight = viewport.height,
                contentWidth = content.width,
                contentHeight = content.height
            )

            // Небольшой допуск для сравнения с 1f
            val isPurePan = kotlin.math.abs(zoom - 1f) < 0.0001f

            if (isPurePan) {
                // === ЧИСТЫЙ ПАН ОДНИМ (ИЛИ ДВУМЯ) ПАЛЬЦАМИ ===
                currentTransform.tx += pan.x
                currentTransform.ty += pan.y
                currentTransform.clampTranslation()

                scale = currentTransform.scale
                tx = currentTransform.tx
                ty = currentTransform.ty
                return
            }

            // === PINCH-ЗУМ (С ОДНОВРЕМЕННЫМ ПАНОМ) ===
            val newScale = (currentTransform.scale * zoom).coerceIn(minScale, maxScale)

            // Привязываем масштаб к точке centroid,
            // чтобы картинка "под пальцами" оставалась на месте
            val imagePoint = currentTransform.screenToImage(centroid)
            currentTransform.scale = newScale
            val newScreenPoint = currentTransform.imageToScreen(imagePoint)

            currentTransform.tx += centroid.x - newScreenPoint.x + pan.x
            currentTransform.ty += centroid.y - newScreenPoint.y + pan.y

            currentTransform.clampTranslation()

            scale = currentTransform.scale
            tx = currentTransform.tx
            ty = currentTransform.ty
        }
    }

}

data class EditLandmarksUiState(
    val resultId: String = "",
    val imagePath: String = "",
    val landmarkSet: LandmarkSet? = null,
    val isLoading: Boolean = true,
    val error: Boolean = false,
    val activePoint: AnatomicalPoint? = null,
    val showMagnifier: Boolean = false,
    val magnifierCenter: Offset = Offset(0.5f, 0.5f),
    val showHelpSheet: Boolean = false,
    val helpText: String = "",
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val canUndo: Boolean = false,
    val side: Side = Side.FRONT
) {
    val allPoints: List<Landmark> get() = landmarkSet?.points.orEmpty()
//    val basePoints: List<Landmark> get() = landmarkSet?.baseOnly().orEmpty()
//    val showReference: Boolean get() = activePoint != null
}

sealed interface EditLandmarksEvent {
    data class NavigateToIndicators(val resultId: String, val imagePath: String) : EditLandmarksEvent
}

private fun <T> ArrayDeque<T>.removeLastOrNull(): T? =
    if (isEmpty()) null else removeLast()
