package com.example.postureapp.ui.processing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.R
import com.example.postureapp.core.vision.PoseModelMissingException
import com.example.postureapp.core.vision.PoseNotFoundException
import com.example.postureapp.data.processing.ProcessingStore
import com.example.postureapp.domain.processing.ProcessCroppedImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    private val processCroppedImageUseCase: ProcessCroppedImageUseCase,
    private val processingStore: ProcessingStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProcessingUiState(
            progressText = context.getString(R.string.processing_message)
        )
    )
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProcessingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentPath: String? = null
    private var currentResultId: String? = null
    private var processJob: Job? = null

    fun start(path: String, resultId: String? = null) {
        if (currentPath == path && _uiState.value.isLoading) return
        currentPath = path
        currentResultId = resultId
        process(path, resultId)
    }

    fun retry() {
        val path = currentPath
        if (path.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = context.getString(R.string.processing_error_missing_path)
                )
            }
            return
        }
        process(path)
    }

    private fun process(path: String, resultIdArg: String? = null) {
        if (path.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = context.getString(R.string.processing_error_missing_path)
                )
            }
            return
        }

        processJob?.cancel()
        processJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            runCatching {
                processCroppedImageUseCase(path)
            }.onSuccess { landmarkSet ->
                val resultId = resultIdArg?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                processingStore.put(resultId, landmarkSet)
                _events.send(ProcessingEvent.NavigateToEdit(resultId = resultId, imagePath = path))
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = mapError(throwable)
                    )
                }
            }
        }
    }

    private fun mapError(throwable: Throwable): String {
        return when (throwable) {
            is PoseNotFoundException -> context.getString(R.string.processing_error_no_pose)
            is PoseModelMissingException -> context.getString(R.string.processing_error_missing_model)
            is IllegalArgumentException -> context.getString(R.string.processing_error_missing_path)
            else -> context.getString(R.string.processing_error_generic)
        }
    }
}

data class ProcessingUiState(
    val isLoading: Boolean = true,
    val progressText: String = "",
    val error: String? = null
)

sealed interface ProcessingEvent {
    data class NavigateToEdit(
        val resultId: String,
        val imagePath: String
    ) : ProcessingEvent
}


