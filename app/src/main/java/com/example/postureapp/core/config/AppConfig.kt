package com.example.postureapp.core.config

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class AppConfig @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json
) {

    private val config: ConfigPayload = context.assets
        .open(CONFIG_FILE_NAME)
        .bufferedReader()
        .use { reader ->
            json.decodeFromString(reader.readText())
        }

    val apiBaseUrl: String = config.apiBaseUrl.trimEnd('/')
    val healthPath: String = config.healthPath
    val healthUrl: String get() = resolve(healthPath)

    val authHeaderName: String = config.auth.header
    val tokenPrefix: String = config.auth.tokenPrefix

    val registerUrl: String get() = resolve(config.auth.register)
    val loginUrl: String get() = resolve(config.auth.login)
    val refreshUrl: String get() = resolve(config.auth.refresh)
    val verifyUrl: String get() = resolve(config.auth.verify)
    val meUrl: String get() = resolve(config.auth.me)

    val connectTimeoutMillis: Long = (config.timeouts?.connectSeconds ?: DEFAULT_CONNECT_SECONDS) * 1_000L
    val readTimeoutMillis: Long = (config.timeouts?.readSeconds ?: DEFAULT_READ_SECONDS) * 1_000L

    private fun resolve(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        val normalized = if (path.startsWith("/")) path else "/$path"
        return apiBaseUrl + normalized
    }

    @Serializable
    private data class ConfigPayload(
        val environment: String,
        @SerialName("api_base_url") val apiBaseUrl: String,
        @SerialName("health_path") val healthPath: String,
        val auth: AuthConfig,
        val timeouts: TimeoutConfig? = null
    )

    @Serializable
    data class AuthConfig(
        val register: String,
        val login: String,
        val refresh: String,
        val verify: String,
        val me: String,
        val header: String,
        @SerialName("token_prefix") val tokenPrefix: String
    )

    @Serializable
    data class TimeoutConfig(
        @SerialName("connect_seconds") val connectSeconds: Long? = null,
        @SerialName("read_seconds") val readSeconds: Long? = null
    )

    companion object {
        private const val CONFIG_FILE_NAME = "android-client-config.json"
        private const val DEFAULT_CONNECT_SECONDS = 10L
        private const val DEFAULT_READ_SECONDS = 30L
    }
}

