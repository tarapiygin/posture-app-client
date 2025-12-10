package com.example.postureapp.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.data.reports.ReportEntity
import com.example.postureapp.domain.reports.ReportRepository
import com.example.postureapp.pdf.ReportShare
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
    private val share: ReportShare
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsListUiState())
    val uiState: StateFlow<ReportsListUiState> = _uiState.asStateFlow()

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
            repository.delete(report)
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
}

