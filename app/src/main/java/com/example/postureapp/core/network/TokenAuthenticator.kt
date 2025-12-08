package com.example.postureapp.core.network

import com.example.postureapp.core.config.AppConfig
import com.example.postureapp.core.datastore.SecureTokenStore
import com.example.postureapp.data.auth.AuthApi
import com.example.postureapp.data.auth.dto.AuthRefreshRequestDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: SecureTokenStore,
    private val appConfig: AppConfig,
    @javax.inject.Named("RefreshAuthApi") private val refreshApi: dagger.Lazy<AuthApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_RETRY_COUNT) {
            runBlocking { tokenStore.clear() }
            return null
        }

        val refreshToken = tokenStore.currentTokens()?.refreshToken ?: return null
        val newAccess = runBlocking {
            try {
                val refreshResponse = refreshApi.get().refresh(
                    appConfig.refreshUrl,
                    AuthRefreshRequestDto(refreshToken)
                )
                tokenStore.updateAccessToken(refreshResponse.access)
                refreshResponse.access
            } catch (_: Exception) {
                tokenStore.clear()
                null
            }
        } ?: return null

        return response.request.newBuilder()
            .header(appConfig.authHeaderName, "${appConfig.tokenPrefix}$newAccess")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var currentResponse: Response? = response
        var count = 1
        while (currentResponse?.priorResponse != null) {
            currentResponse = currentResponse.priorResponse
            count++
        }
        return count
    }

    companion object {
        private const val MAX_RETRY_COUNT = 2
    }
}

