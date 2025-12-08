package com.example.postureapp.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onStartAnalysis() {
        _uiState.update { it.copy(permissionDialogVisible = false, permissionError = null) }
    }

    fun onPermissionsDenied(message: String?) {
        _uiState.update {
            it.copy(
                permissionDialogVisible = true,
                permissionError = message
            )
        }
    }

    fun onOpenSettings() {
        _uiState.update { it.copy(permissionDialogVisible = false) }
    }

    fun onDismissPermissionDialog() {
        _uiState.update { it.copy(permissionDialogVisible = false) }
    }
}

data class HomeUiState(
    val permissionDialogVisible: Boolean = false,
    val permissionError: String? = null
)

