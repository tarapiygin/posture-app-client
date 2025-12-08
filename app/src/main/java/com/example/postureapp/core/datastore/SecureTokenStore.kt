package com.example.postureapp.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

private val Context.secureTokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "secure_tokens"
)

@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cryptoManager: CryptoManager
) {

    private val dataStore = context.secureTokenDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    val tokens: StateFlow<TokenBundle?> = dataStore.data
        .map { prefs ->
            val accessEncrypted = prefs[accessKey]
            val refreshEncrypted = prefs[refreshKey]
            if (accessEncrypted.isNullOrBlank() || refreshEncrypted.isNullOrBlank()) {
                null
            } else {
                val access = cryptoManager.decrypt(accessEncrypted)
                val refresh = cryptoManager.decrypt(refreshEncrypted)
                if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
                    null
                } else {
                    TokenBundle(access, refresh)
                }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun saveTokens(access: String, refresh: String) {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[accessKey] = cryptoManager.encrypt(access)
                prefs[refreshKey] = cryptoManager.encrypt(refresh)
            }
        }
    }

    suspend fun updateAccessToken(access: String) {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                if (prefs[refreshKey] != null) {
                    prefs[accessKey] = cryptoManager.encrypt(access)
                }
            }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs.clear()
            }
        }
    }

    fun currentTokens(): TokenBundle? = tokens.value
    fun currentAccessToken(): String? = tokens.value?.accessToken

    data class TokenBundle(
        val accessToken: String,
        val refreshToken: String
    )
}

