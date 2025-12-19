package com.example.postureapp.core.network.api

import com.example.postureapp.data.reports.sync.dto.PhotoUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PhotosApi {
    @Multipart
    @POST("api/photos/upload/{session_id}/")
    suspend fun uploadToSession(
        @Path("session_id") sessionId: String,
        @Part file: MultipartBody.Part,
        @Part("view") view: RequestBody
    ): PhotoUploadResponse
}

