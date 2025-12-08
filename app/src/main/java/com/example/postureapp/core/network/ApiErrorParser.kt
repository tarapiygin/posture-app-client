package com.example.postureapp.core.network

import com.example.postureapp.data.auth.dto.ProblemDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.HttpException

@Singleton
class ApiErrorParser @Inject constructor(
    private val json: Json
) {

    fun parse(throwable: Throwable): ProblemDto? {
        if (throwable !is HttpException) return null
        val body = throwable.response()?.errorBody()
        return parse(body)
    }

    fun parse(body: ResponseBody?): ProblemDto? {
        if (body == null) return null
        return try {
            val content = body.string()
            if (content.isBlank()) null else json.decodeFromString(ProblemDto.serializer(), content)
        } catch (_: SerializationException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}

