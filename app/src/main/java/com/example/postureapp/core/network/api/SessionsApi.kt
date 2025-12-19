package com.example.postureapp.core.network.api

import com.example.postureapp.data.reports.sync.dto.SessionResolveRequest
import com.example.postureapp.data.reports.sync.dto.SessionResolveResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SessionsApi {
    @POST("api/sessions/resolve/")
    suspend fun resolve(@Body body: SessionResolveRequest): SessionResolveResponse
}

