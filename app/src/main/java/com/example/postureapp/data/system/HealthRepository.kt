package com.example.postureapp.data.system

import com.example.postureapp.core.config.AppConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val healthApi: HealthApi,
    private val appConfig: AppConfig
) {
    suspend fun ping(): Boolean {
        return try {
            healthApi.health(appConfig.healthUrl)
            true
        } catch (_: Exception) {
            false
        }
    }
}

