package com.example.postureapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.data.auth.AuthRepository
import com.example.postureapp.data.system.HealthRepository
import com.example.postureapp.domain.auth.AuthUseCases
import com.example.postureapp.ui.navigation.AppDestination
import com.example.postureapp.ui.navigation.MainGraphRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authUseCases: AuthUseCases,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val authState = authRepository.authState
    val currentUser = authRepository.currentUser

    init {
        viewModelScope.launch { resolveStartDestination() }
        viewModelScope.launch { checkBackendHealth() }
    }

    private suspend fun resolveStartDestination() {
        val hasSession = authRepository.hasStoredSession()
        val destination = if (hasSession) {
            val meResult = authUseCases.loadMe()
            if (meResult.isSuccess) {
                MainGraphRoute
            } else {
                authUseCases.logout()
                AppDestination.AuthRoot.route
            }
        } else {
            AppDestination.AuthRoot.route
        }
        _uiState.update { it.copy(startDestination = destination) }
    }

    private suspend fun checkBackendHealth() {
        val healthy = healthRepository.ping()
        _uiState.update {
            it.copy(
                backendStatus = when {
                    healthy -> BackendStatus.Healthy
                    else -> BackendStatus.Unreachable
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authUseCases.logout()
        }
    }
}

data class MainUiState(
    val startDestination: String? = null,
    val backendStatus: BackendStatus = BackendStatus.Unknown
)

enum class BackendStatus {
    Unknown,
    Healthy,
    Unreachable
}

