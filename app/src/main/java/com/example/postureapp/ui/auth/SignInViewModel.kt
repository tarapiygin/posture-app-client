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
class SignInViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignInEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        val emailError = InputValidators.validateEmail(state.email)
        val passwordError = InputValidators.validatePassword(state.password, minLength = 6)

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authUseCases.signIn(state.email, state.password)
            result
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(SignInEvent.NavigateHome)
                }
                .onFailure { throwable ->
                    val message = (throwable as? ProblemException)?.problem?.detail
                        ?: throwable.message
                        ?: "Unable to sign in"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }
}

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() = InputValidators.validateEmail(email) == null &&
            InputValidators.validatePassword(password, minLength = 6) == null &&
            !isLoading
}

sealed interface SignInEvent {
    data object NavigateHome : SignInEvent
}

