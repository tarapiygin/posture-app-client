package com.example.postureapp.ui.results

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.R
import com.example.postureapp.core.draw.FrontLevel
import com.example.postureapp.core.draw.ReportTileRenderer
import com.example.postureapp.core.draw.RightSegment
import com.example.postureapp.core.report.Side
import com.example.postureapp.data.processing.ProcessingStore
import com.example.postureapp.data.report.ReportCoordinator
import com.example.postureapp.data.report.SideState
import com.example.postureapp.data.reports.ReportConverters
import com.example.postureapp.data.reports.ReportEntity
import com.example.postureapp.domain.metrics.ComputeFrontMetricsUseCase
import com.example.postureapp.domain.metrics.FrontMetrics
import com.example.postureapp.domain.metrics.right.ComputeRightMetricsUseCase
import com.example.postureapp.domain.metrics.right.RightMetrics
import com.example.postureapp.domain.reports.ReportRepository
import com.example.postureapp.pdf.FrontRenderData
import com.example.postureapp.pdf.ReportPdfBuilder
import com.example.postureapp.pdf.ReportRenderData
import com.example.postureapp.pdf.ReportShare
import com.example.postureapp.pdf.RightRenderData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@HiltViewModel
class ResultsTabViewModel @Inject constructor(
    private val coordinator: ReportCoordinator,
    private val processingStore: ProcessingStore,
    private val computeFrontMetrics: ComputeFrontMetricsUseCase,
    private val computeRightMetrics: ComputeRightMetricsUseCase,
    private val pdfBuilder: ReportPdfBuilder,
    private val reportRepository: ReportRepository,
    private val shareHelper: ReportShare,
    @ApplicationContext private val context: Context,
    json: Json
) : ViewModel() {

    private val converters = ReportConverters(json)

    private val _uiState = MutableStateFlow(ResultsTabUiState())
    val uiState: StateFlow<ResultsTabUiState> = _uiState.asStateFlow()

    private val _events = Channel<ResultsTabEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            coordinator.sessionFlow.collectLatest { session ->
                val frontSide = session.front
                val rightSide = session.right
                val frontData = buildFrontData(frontSide)
                val rightData = buildRightData(rightSide)
                val renderData = ReportRenderData(
                    sessionId = session.id,
                    createdAt = System.currentTimeMillis(),
                    front = frontData,
                    right = rightData
                )
                val previews = generatePreviews(frontData, rightData)
                _uiState.update {
                    it.copy(
                        renderData = renderData,
                        front = frontData,
                        right = rightData,
                        preview = previews,
                        canShare = frontData != null || rightData != null,
                        canSave = (frontSide.hasFinal && frontData != null) || (rightSide.hasFinal && rightData != null),
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun buildFrontData(side: SideState): FrontRenderData? = withContext(Dispatchers.Default) {
        if (side.side != Side.FRONT) return@withContext null
        val rid = side.resultId ?: return@withContext null
        val landmarks = processingStore.getFinal(rid) ?: processingStore.getAuto(rid) ?: return@withContext null
        val metrics = computeFrontMetrics(landmarks) ?: return@withContext null
        FrontRenderData(
            imagePath = side.croppedPath.orEmpty(),
            landmarks = landmarks,
            metrics = metrics
        )
    }

    private suspend fun buildRightData(side: SideState): RightRenderData? = withContext(Dispatchers.Default) {
        if (side.side != Side.RIGHT) return@withContext null
        val rid = side.resultId ?: return@withContext null
        val landmarks = processingStore.getFinal(rid) ?: processingStore.getAuto(rid) ?: return@withContext null
        val metrics = computeRightMetrics(landmarks) ?: return@withContext null
        RightRenderData(
            imagePath = side.croppedPath.orEmpty(),
            landmarks = landmarks,
            metrics = metrics
        )
    }

    private suspend fun generatePreviews(
        front: FrontRenderData?,
        right: RightRenderData?
    ): ReportPreview? = withContext(Dispatchers.Default) {
        if (front == null && right == null) return@withContext null
        val frontPanel = front?.let {
            ReportTileRenderer.renderFrontPanel(it.imagePath, it.metrics, landmarks = it.landmarks, width = 480, height = 640)
        }
        val rightPanel = right?.let {
            ReportTileRenderer.renderRightPanel(it.imagePath, it.metrics, landmarks = it.landmarks, width = 480, height = 640)
        }
        val frontTiles = front?.let { data ->
            listOf(
                FrontLevel.EARS,
                FrontLevel.SHOULDERS,
                FrontLevel.ASIS,
                FrontLevel.KNEES,
                FrontLevel.FEET
            ).mapNotNull { level ->
                ReportTileRenderer.renderFrontTile(level, data.imagePath, data.metrics, landmarks = data.landmarks, size = 360)?.let { bmp ->
                    FrontTilePreview(level, bmp)
                }
            }
        }
//        val rightTiles = right?.let { data ->
//            listOf(
//                RightSegment.CVA,
//                RightSegment.KNEE,
//                RightSegment.HIP,
//                RightSegment.SHOULDER,
//                RightSegment.EAR
//            ).mapNotNull { segment ->
//                ReportTileRenderer.renderRightTile(segment, data.imagePath, data.metrics, landmarks = data.landmarks, size = 360)?.let { bmp ->
//                    RightTilePreview(segment, bmp)
//                }
//            }
//        }
//        ReportPreview(frontPanel, rightPanel, frontTiles.orEmpty(), rightTiles.orEmpty())
        ReportPreview(frontPanel, rightPanel, frontTiles.orEmpty())
    }

    fun onShare() {
        val renderData = _uiState.value.renderData ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(sharing = true, error = null) }
            val dir = File(context.cacheDir, "reports_tmp").apply { mkdirs() }
            val outFile = File(dir, "REPORT_tmp.pdf")
            val result = pdfBuilder.build(renderData, outFile)
            result.onSuccess { file ->
                shareHelper.sharePdf(file)
                _events.send(ResultsTabEvent.Toast(context.getString(R.string.share_success)))
            }.onFailure {
                _events.send(ResultsTabEvent.Toast(it.message ?: context.getString(R.string.error_generic)))
            }
            _uiState.update { it.copy(sharing = false) }
        }
    }

    fun onSave() {
        val renderData = _uiState.value.renderData ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val now = System.currentTimeMillis()
            val reportId = UUID.randomUUID().toString()
            val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
            val outFile = File(reportsDir, "REPORT_${now}.pdf")
            val pdfResult = pdfBuilder.build(renderData, outFile)
            val thumb = generateThumbnail(renderData)
            pdfResult.onSuccess { file ->
                val entity = ReportEntity(
                    id = reportId,
                    createdAt = now,
                    updatedAt = now,
                    sessionId = renderData.sessionId,
                    frontImagePath = renderData.front?.imagePath,
                    rightImagePath = renderData.right?.imagePath,
                    frontLandmarksJson = converters.encodeLandmarks(renderData.front?.landmarks),
                    rightLandmarksJson = converters.encodeLandmarks(renderData.right?.landmarks),
                    frontMetricsJson = converters.encodeFrontMetrics(renderData.front?.metrics),
                    rightMetricsJson = converters.encodeRightMetrics(renderData.right?.metrics),
                    pdfPath = file.absolutePath,
                    thumbnailPath = thumb?.absolutePath,
                    serverId = null,
                    syncStatus = "PENDING",
                    version = 1,
                    userId = null
                )
                reportRepository.upsert(entity)
                coordinator.reset()
                _events.send(ResultsTabEvent.NavigateToReports)
            }.onFailure {
                _events.send(ResultsTabEvent.Toast(it.message ?: context.getString(R.string.error_generic)))
            }
            _uiState.update { it.copy(saving = false) }
        }
    }

    private suspend fun generateThumbnail(data: ReportRenderData): File? = withContext(Dispatchers.IO) {
        val bitmap: Bitmap = data.front?.let {
            ReportTileRenderer.renderFrontPanel(it.imagePath, it.metrics, landmarks = it.landmarks, width = 360, height = 480)
        } ?: data.right?.let {
            ReportTileRenderer.renderRightPanel(it.imagePath, it.metrics, landmarks = it.landmarks, width = 360, height = 480)
        } ?: return@withContext null

        val thumbsDir = File(context.filesDir, "reports/thumbs").apply { mkdirs() }
        val file = File(thumbsDir, "thumb_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file
    }
}

data class ResultsTabUiState(
    val isLoading: Boolean = true,
    val renderData: ReportRenderData? = null,
    val front: FrontRenderData? = null,
    val right: RightRenderData? = null,
    val preview: ReportPreview? = null,
    val canShare: Boolean = false,
    val canSave: Boolean = false,
    val saving: Boolean = false,
    val sharing: Boolean = false,
    val error: String? = null
)

data class ReportPreview(
    val frontPanel: Bitmap?,
    val rightPanel: Bitmap?,
    val frontTiles: List<FrontTilePreview>,
//    val rightTiles: List<RightTilePreview>
)

data class FrontTilePreview(
    val level: FrontLevel,
    val bitmap: Bitmap
)

data class RightTilePreview(
    val segment: RightSegment,
    val bitmap: Bitmap
)

sealed interface ResultsTabEvent {
    data object NavigateToReports : ResultsTabEvent
    data class Toast(val message: String) : ResultsTabEvent
}

