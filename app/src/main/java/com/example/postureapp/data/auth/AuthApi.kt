package com.example.postureapp.data.auth

import com.example.postureapp.data.auth.dto.AuthLoginRequestDto
import com.example.postureapp.data.auth.dto.AuthLoginResponseDto
import com.example.postureapp.data.auth.dto.AuthRefreshRequestDto
import com.example.postureapp.data.auth.dto.AuthRefreshResponseDto
import com.example.postureapp.data.auth.dto.AuthRegisterRequestDto
import com.example.postureapp.data.auth.dto.AuthRegisterResponseDto
import com.example.postureapp.data.auth.dto.AuthVerifyRequestDto
import com.example.postureapp.data.auth.dto.AuthVerifyResponseDto
import com.example.postureapp.data.auth.dto.UserMeDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface AuthApi {

    @POST
    suspend fun register(
        @Url url: String,
        @Body request: AuthRegisterRequestDto
    ): AuthRegisterResponseDto

    @POST
    suspend fun login(
        @Url url: String,
        @Body request: AuthLoginRequestDto
    ): AuthLoginResponseDto

    @POST
    suspend fun refresh(
        @Url url: String,
        @Body request: AuthRefreshRequestDto
    ): AuthRefreshResponseDto

    @POST
    suspend fun verify(
        @Url url: String,
        @Body request: AuthVerifyRequestDto
    ): AuthVerifyResponseDto

    @GET
    suspend fun me(
        @Url url: String
    ): UserMeDto
}

