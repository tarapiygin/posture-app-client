package com.example.postureapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.core.utils.InputValidators
import com.example.postureapp.data.auth.ProblemException
import com.example.postureapp.domain.auth.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignUpEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(email = value, emailError = null, errorMessage = null)
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(password = value, passwordError = null, errorMessage = null)
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null)
        }
    }

    fun submit() {
        val state = _uiState.value
        val emailError = InputValidators.validateEmail(state.email)
        val passwordError = InputValidators.validatePassword(state.password, minLength = 8)
        val confirmError = when {
            state.confirmPassword.isBlank() -> "Please confirm password"
            state.confirmPassword != state.password -> "Passwords do not match"
            else -> null
        }

        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authUseCases.signUp(state.email, state.password)
            result
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(SignUpEvent.NavigateHome)
                }
                .onFailure { throwable ->
                    val message = (throwable as? ProblemException)?.problem?.detail
                        ?: throwable.message
                        ?: "Unable to create account"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }
}

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() = InputValidators.validateEmail(email) == null &&
            InputValidators.validatePassword(password, minLength = 8) == null &&
            confirmPassword == password &&
            confirmPassword.isNotBlank() &&
            !isLoading
}

sealed interface SignUpEvent {
    data object NavigateHome : SignUpEvent
}

