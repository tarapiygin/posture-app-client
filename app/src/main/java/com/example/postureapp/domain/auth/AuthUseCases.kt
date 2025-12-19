package com.example.postureapp.domain.auth

import com.example.postureapp.data.auth.AuthRepository
import com.example.postureapp.domain.cleanup.ClearLocalDataUseCase
import com.example.postureapp.domain.auth.model.AuthTokens
import com.example.postureapp.domain.auth.model.User
import javax.inject.Inject

data class AuthUseCases @Inject constructor(
    val signIn: SignInUseCase,
    val signUp: SignUpUseCase,
    val refreshToken: RefreshTokenUseCase,
    val loadMe: LoadMeUseCase,
    val logout: LogoutUseCase,
    val verifyToken: VerifyTokenUseCase
)

class SignInUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        repository.login(email, password)
}

class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        repository.register(email, password)
}

class RefreshTokenUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<AuthTokens> = repository.refreshTokens()
}

class LoadMeUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<User> = repository.loadProfile()
}

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val clearLocalDataUseCase: ClearLocalDataUseCase
) {
    suspend operator fun invoke() {
        clearLocalDataUseCase()
        repository.logout()
    }
}

class VerifyTokenUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<String> = repository.verifyActiveToken()
}

