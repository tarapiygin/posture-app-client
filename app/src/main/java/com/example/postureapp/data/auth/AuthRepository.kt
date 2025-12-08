package com.example.postureapp.data.auth

import com.example.postureapp.core.config.AppConfig
import com.example.postureapp.core.datastore.SecureTokenStore
import com.example.postureapp.core.network.ApiErrorParser
import com.example.postureapp.data.auth.dto.AuthLoginRequestDto
import com.example.postureapp.data.auth.dto.AuthRefreshRequestDto
import com.example.postureapp.data.auth.dto.AuthRegisterRequestDto
import com.example.postureapp.data.auth.dto.AuthVerifyRequestDto
import com.example.postureapp.data.auth.dto.ProblemDto
import com.example.postureapp.domain.auth.model.AuthTokens
import com.example.postureapp.domain.auth.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: SecureTokenStore,
    private val appConfig: AppConfig,
    private val errorParser: ApiErrorParser
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        scope.launch {
            tokenStore.tokens.collect { tokens ->
                if (tokens == null) {
                    _authState.value = AuthState.SignedOut
                    _currentUser.value = null
                } else {
                    _authState.value = AuthState.Authenticated
                }
            }
        }
    }

    suspend fun register(email: String, password: String): Result<User> {
        val result = executeAuthCall {
            val response = authApi.register(appConfig.registerUrl, AuthRegisterRequestDto(email, password))
            persistTokens(response.tokens.access, response.tokens.refresh)
            val user = response.user.toDomain()
            _currentUser.value = user
            user
        }
        if (result.isFailure) {
            _authState.value = AuthState.SignedOut
        }
        return result
    }

    suspend fun login(email: String, password: String): Result<User> {
        val result = executeAuthCall {
            val response = authApi.login(appConfig.loginUrl, AuthLoginRequestDto(email, password))
            persistTokens(response.access, response.refresh)
            fetchProfileInternal()
        }
        if (result.isFailure) {
            _authState.value = AuthState.SignedOut
        }
        return result
    }

    suspend fun loadProfile(): Result<User> = executeAuthCall { fetchProfileInternal() }

    suspend fun verifyActiveToken(): Result<String> {
        val access = tokenStore.currentAccessToken()
            ?: return Result.failure(IllegalStateException("No active token"))
        return executeAuthCall {
            authApi.verify(appConfig.verifyUrl, AuthVerifyRequestDto(access)).detail
        }
    }

    suspend fun refreshTokens(): Result<AuthTokens> {
        val refreshToken = tokenStore.currentTokens()?.refreshToken
            ?: return Result.failure(IllegalStateException("Missing refresh token"))
        return executeAuthCall {
            val response = authApi.refresh(appConfig.refreshUrl, AuthRefreshRequestDto(refreshToken))
            tokenStore.updateAccessToken(response.access)
            AuthTokens(access = response.access, refresh = refreshToken)
        }
    }

    suspend fun logout() {
        tokenStore.clear()
        _authState.value = AuthState.SignedOut
        _currentUser.value = null
    }

    suspend fun hasStoredSession(): Boolean = tokenStore.currentTokens() != null

    private suspend fun fetchProfileInternal(): User {
        val dto = authApi.me(appConfig.meUrl)
        return dto.toDomain().also { _currentUser.value = it }
    }

    private suspend fun persistTokens(access: String, refresh: String) {
        tokenStore.saveTokens(access, refresh)
    }

    private suspend fun <T> executeAuthCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (throwable: Throwable) {
            Result.failure(mapError(throwable))
        }
    }

    private fun mapError(throwable: Throwable): Throwable {
        val problem = errorParser.parse(throwable)
        return if (problem != null) {
            ProblemException(problem)
        } else {
            throwable
        }
    }
}

sealed interface AuthState {
    data object SignedOut : AuthState
    data object Authenticated : AuthState
}

class ProblemException(val problem: ProblemDto) : Exception(problem.detail)

