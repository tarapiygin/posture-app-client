package com.example.postureapp.data.auth.dto

import com.example.postureapp.domain.auth.model.AuthTokens
import com.example.postureapp.domain.auth.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthLoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class AuthLoginResponseDto(
    val access: String,
    val refresh: String
) {
    fun toDomain(): AuthTokens = AuthTokens(access = access, refresh = refresh)
}

@Serializable
data class AuthRegisterRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class AuthRegisterResponseDto(
    val user: UserMeDto,
    val tokens: AuthTokensDto
) {
    fun toDomainUser(): User = user.toDomain()
    fun toDomainTokens(): AuthTokens = tokens.toDomain()
}

@Serializable
data class AuthTokensDto(
    val access: String,
    val refresh: String
) {
    fun toDomain(): AuthTokens = AuthTokens(access = access, refresh = refresh)
}

@Serializable
data class AuthRefreshRequestDto(
    val refresh: String
)

@Serializable
data class AuthRefreshResponseDto(
    val access: String
)

@Serializable
data class AuthVerifyRequestDto(
    val token: String
)

@Serializable
data class AuthVerifyResponseDto(
    val detail: String
)

@Serializable
data class UserMeDto(
    val id: String,
    val email: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("is_superuser") val isSuperuser: Boolean
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        isActive = isActive,
        isSuperuser = isSuperuser
    )
}

@Serializable
data class ProblemDto(
    val detail: String,
    val code: String? = null,
    val errors: List<ProblemErrorDto>? = null
)

@Serializable
data class ProblemErrorDto(
    val loc: List<String>? = null,
    val msg: String
)

