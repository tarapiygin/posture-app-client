package com.example.postureapp.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.data.reports.ReportEntity
import com.example.postureapp.domain.reports.ReportRepository
import com.example.postureapp.domain.pdf.ReportShare
import com.example.postureapp.domain.reports.SyncAllReportsUseCase
import com.example.postureapp.domain.reports.DeleteReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ReportsListViewModel @Inject constructor(
    private val repository: ReportRepository,
    private val share: ReportShare,
    private val syncReportsUseCase: SyncAllReportsUseCase,
    private val deleteReportUseCase: DeleteReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsListUiState())
    val uiState: StateFlow<ReportsListUiState> = _uiState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow<String?>(null)
    val lastSyncSummary: StateFlow<String?> = _lastSyncSummary.asStateFlow()

    private val _events = Channel<ReportsListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val reports = repository.getReports()
            _uiState.value = ReportsListUiState(
                isLoading = false,
                reports = reports
            )
        }
    }

    fun onSyncClick() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = syncReportsUseCase()
                when (result) {
                    is SyncAllReportsUseCase.Result.NetworkUnavailable -> {
                        _events.send(ReportsListEvent.NetworkUnavailable)
                    }

                    is SyncAllReportsUseCase.Result.Success -> {
                        val summary = "Uploaded: ${result.uploaded}, Pulled: ${result.pulled}, Failed: ${result.failed}"
                        _lastSyncSummary.value = summary
                        _events.send(
                            ReportsListEvent.SyncSummary(
                                uploaded = result.uploaded,
                                pulled = result.pulled,
                                failed = result.failed
                            )
                        )
                    }

                    is SyncAllReportsUseCase.Result.Error -> {
                        _events.send(
                            ReportsListEvent.Toast(
                                result.message.ifBlank { "Sync failed" }
                            )
                        )
                    }
                }
                refresh()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun share(report: ReportEntity) {
        viewModelScope.launch {
            if (report.pdfPath.isBlank()) {
                _events.send(ReportsListEvent.Toast("PDF missing"))
                return@launch
            }
            share.sharePdf(java.io.File(report.pdfPath))
        }
    }

    fun delete(report: ReportEntity) {
        viewModelScope.launch {
            runCatching { deleteReportUseCase(report) }
                .onFailure {
                    _events.send(ReportsListEvent.Toast(it.message ?: "Delete failed"))
                }
            refresh()
        }
    }
}

data class ReportsListUiState(
    val isLoading: Boolean = false,
    val reports: List<ReportEntity> = emptyList()
)

sealed interface ReportsListEvent {
    data class Toast(val message: String) : ReportsListEvent
    data object NetworkUnavailable : ReportsListEvent
    data class SyncSummary(val uploaded: Int, val pulled: Int, val failed: Int) : ReportsListEvent
}

