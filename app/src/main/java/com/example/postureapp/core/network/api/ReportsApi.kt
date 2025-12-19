package com.example.postureapp.core.network.api

import com.example.postureapp.data.reports.sync.dto.ReportUploadResponse
import com.example.postureapp.data.reports.sync.dto.ReportsDeltaRequest
import com.example.postureapp.data.reports.sync.dto.ReportsDeltaResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Part

interface ReportsApi {

    @Multipart
    @POST("api/reports/")
    suspend fun uploadReport(
        @Part pdf: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody
    ): ReportUploadResponse

    @POST("api/reports/delta/")
    suspend fun delta(@Body body: ReportsDeltaRequest): ReportsDeltaResponse

    @DELETE("api/reports/{id}/")
    suspend fun delete(@Path("id") serverId: String)
}

