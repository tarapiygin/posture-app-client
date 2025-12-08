package com.example.postureapp.data.system

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface HealthApi {
    @GET
    suspend fun health(@Url url: String): ResponseBody
}

