package com.example.postureapp.ui.capture.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.core.camera.OrientationAngles
import com.example.postureapp.core.camera.OrientationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@HiltViewModel
class PhotoCaptureViewModel @Inject constructor(
    private val orientationManager: OrientationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private var orientationJob: Job? = null

    fun onAppear() {
        orientationManager.start()
        if (orientationJob?.isActive == true) return
        orientationJob = orientationManager.angles
            .onEach { updateOrientation(it) }
            .launchIn(viewModelScope)
    }

    fun onDisappear() {
        orientationManager.stop()
    }

    fun onShoot(): Boolean {
        val current = _uiState.value
        return if (!current.isCapturing) {
            _uiState.update { it.copy(isCapturing = true) }
            true
        } else {
            false
        }
    }

    fun onCaptureFinished(errorMessage: String? = null) {
        _uiState.update {
            it.copy(
                isCapturing = false,
                error = errorMessage
            )
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    private fun updateOrientation(angles: OrientationAngles) {
        val pitch = angles.pitchDeg
        val roll = angles.rollDeg
        val aligned = abs(roll) <= ROLL_TOLERANCE && abs(pitch) <= PITCH_TOLERANCE
        _uiState.update {
            it.copy(
                pitch = pitch,
                roll = roll,
                aligned = aligned
            )
        }
    }

    companion object {
        const val ROLL_TOLERANCE = 2f
        const val PITCH_TOLERANCE = 5f
    }
}

data class AnalysisUiState(
    val isCapturing: Boolean = false,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val aligned: Boolean = false,
    val error: String? = null
)


