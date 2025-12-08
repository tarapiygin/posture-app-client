package com.example.postureapp.core.network

import com.example.postureapp.core.config.AppConfig
import com.example.postureapp.core.datastore.SecureTokenStore
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: SecureTokenStore,
    private val appConfig: AppConfig
) : Interceptor {

    private val excludedEndpoints: Set<String> by lazy {
        setOf(
            appConfig.loginUrl,
            appConfig.registerUrl,
            appConfig.refreshUrl,
            appConfig.verifyUrl
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        if (excludedEndpoints.any { url.startsWith(it) }) {
            return chain.proceed(originalRequest)
        }

        val token = tokenStore.currentAccessToken()
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val updatedRequest = originalRequest.newBuilder()
            .header(appConfig.authHeaderName, "${appConfig.tokenPrefix}$token")
            .build()
        return chain.proceed(updatedRequest)
    }
}

