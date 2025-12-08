package com.example.postureapp.core.network

import com.example.postureapp.core.config.AppConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object OkHttpProvider {
    fun create(
        appConfig: AppConfig,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(appConfig.connectTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(appConfig.readTimeoutMillis, TimeUnit.MILLISECONDS)
            .writeTimeout(appConfig.readTimeoutMillis, TimeUnit.MILLISECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }
}

