package com.example.postureapp.core.network.api

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ReportsSyncApi {

    @Multipart
    @POST("api/reports/")
    suspend fun uploadReport(
        @Part pdf: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody
    ): ReportUploadResponse

    @Multipart
    @PUT("api/reports/{serverId}")
    suspend fun updateReport(
        @Path("serverId") serverId: String,
        @Part pdf: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody
    ): ReportUploadResponse
}

@Serializable
data class ReportUploadResponse(
    val id: String,
    val version: Int? = null
)

