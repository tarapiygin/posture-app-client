package com.example.postureapp.di

import com.example.postureapp.BuildConfig
import com.example.postureapp.core.config.AppConfig
import com.example.postureapp.core.network.api.PhotosApi
import com.example.postureapp.core.network.api.ReportsApi
import com.example.postureapp.core.network.api.SessionsApi
import com.example.postureapp.core.network.AuthInterceptor
import com.example.postureapp.core.network.OkHttpProvider
import com.example.postureapp.core.network.RetrofitProvider
import com.example.postureapp.core.network.TokenAuthenticator
import com.example.postureapp.data.auth.AuthApi
import com.example.postureapp.data.system.HealthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        appConfig: AppConfig,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpProvider.create(
        appConfig = appConfig,
        authInterceptor = authInterceptor,
        tokenAuthenticator = tokenAuthenticator,
        loggingInterceptor = loggingInterceptor
    )

    @Provides
    @Singleton
    @Named("RefreshClient")
    fun provideRefreshClient(
        appConfig: AppConfig,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(appConfig.connectTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(appConfig.readTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(appConfig.readTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        json: Json,
        okHttpClient: OkHttpClient,
        appConfig: AppConfig
    ): Retrofit = RetrofitProvider.create(
        json = json,
        okHttpClient = okHttpClient,
        baseUrl = appConfig.apiBaseUrl
    )

    @Provides
    @Singleton
    @Named("RefreshRetrofit")
    fun provideRefreshRetrofit(
        json: Json,
        @Named("RefreshClient") client: OkHttpClient,
        appConfig: AppConfig
    ): Retrofit = RetrofitProvider.create(
        json = json,
        okHttpClient = client,
        baseUrl = appConfig.apiBaseUrl
    )

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    @Named("RefreshAuthApi")
    fun provideRefreshAuthApi(@Named("RefreshRetrofit") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideHealthApi(retrofit: Retrofit): HealthApi =
        retrofit.create(HealthApi::class.java)

    @Provides
    @Singleton
    fun provideReportsApi(retrofit: Retrofit): ReportsApi =
        retrofit.create(ReportsApi::class.java)

    @Provides
    @Singleton
    fun provideSessionsApi(retrofit: Retrofit): SessionsApi =
        retrofit.create(SessionsApi::class.java)

    @Provides
    @Singleton
    fun providePhotosApi(retrofit: Retrofit): PhotosApi =
        retrofit.create(PhotosApi::class.java)
}

