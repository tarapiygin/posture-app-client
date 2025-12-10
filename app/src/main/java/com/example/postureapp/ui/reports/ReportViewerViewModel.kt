package com.example.postureapp.ui.reports

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.data.reports.ReportEntity
import com.example.postureapp.domain.reports.ReportRepository
import com.example.postureapp.pdf.ReportShare
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ReportViewerViewModel @Inject constructor(
    private val repository: ReportRepository,
    private val share: ReportShare
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportViewerUiState())
    val uiState: StateFlow<ReportViewerUiState> = _uiState

    private val _events = Channel<ReportViewerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val report = repository.getReport(id)
            val pages = report?.pdfPath?.let { renderPdf(it) }.orEmpty()
            _uiState.value = ReportViewerUiState(
                report = report,
                pages = pages,
                isLoading = false
            )
        }
    }

    fun share() {
        val report = _uiState.value.report ?: return
        viewModelScope.launch {
            if (report.pdfPath.isNotBlank()) {
                share.sharePdf(File(report.pdfPath))
            } else {
                _events.send(ReportViewerEvent.Toast("PDF missing"))
            }
        }
    }

    fun delete() {
        val report = _uiState.value.report ?: return
        viewModelScope.launch {
            repository.delete(report)
            _events.send(ReportViewerEvent.Deleted)
        }
    }

    private suspend fun renderPdf(path: String): List<Bitmap> = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext emptyList()
        val rendered = mutableListOf<Bitmap>()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val width = page.width
                        val height = page.height
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        rendered.add(bmp)
                    }
                }
            }
        }
        rendered
    }
}

data class ReportViewerUiState(
    val report: ReportEntity? = null,
    val pages: List<Bitmap> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface ReportViewerEvent {
    data object Deleted : ReportViewerEvent
    data class Toast(val message: String) : ReportViewerEvent
}

