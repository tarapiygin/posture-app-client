package com.example.postureapp.ui.indicators.front

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.data.processing.ProcessingStore
import com.example.postureapp.domain.metrics.ComputeFrontMetricsUseCase
import com.example.postureapp.domain.metrics.FrontMetrics
import com.example.postureapp.domain.landmarks.LandmarkSet
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class FrontIndicatorsViewModel @Inject constructor(
    private val processingStore: ProcessingStore,
    private val computeFrontMetrics: ComputeFrontMetricsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrontIndicatorsUiState())
    val uiState: StateFlow<FrontIndicatorsUiState> = _uiState.asStateFlow()

    fun load(resultId: String, imagePath: String) {
        val cached = _uiState.value
        if (cached.resultId == resultId && cached.imagePath == imagePath && cached.metrics != null) return
        updateAndCompute(resultId, imagePath)
    }

    fun refresh() {
        val current = _uiState.value
        if (current.resultId.isBlank()) return
        updateAndCompute(current.resultId, current.imagePath, force = true)
    }

    private fun updateAndCompute(resultId: String, imagePath: String, force: Boolean = false) {
        viewModelScope.launch {
            if (force) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            } else {
                _uiState.update {
                    it.copy(
                        resultId = resultId,
                        imagePath = imagePath,
                        isLoading = true,
                        error = null
                    )
                }
            }

            val set = withContext(Dispatchers.Default) {
                processingStore.currentFinal(resultId)
            }

            val metrics = set?.let { computeMetrics(it) }

            _uiState.update {
                it.copy(
                    resultId = resultId,
                    imagePath = imagePath,
                    landmarks = set,
                    metrics = metrics,
                    isLoading = false,
                    error = if (metrics == null) "Missing landmarks" else null
                )
            }
        }
    }

    private suspend fun computeMetrics(set: LandmarkSet): FrontMetrics? =
        withContext(Dispatchers.Default) {
            computeFrontMetrics(set)
        }
}

data class FrontIndicatorsUiState(
    val resultId: String = "",
    val imagePath: String = "",
    val landmarks: LandmarkSet? = null,
    val metrics: FrontMetrics? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

