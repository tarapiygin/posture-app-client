package com.example.postureapp.core.navigation

import android.net.Uri
import android.util.Base64
import kotlin.text.Charsets

fun encodePath(path: String): String {
    val encoded = Base64.encodeToString(path.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    return Uri.encode(encoded)
}

fun decodePath(argument: String?): String? {
    if (argument.isNullOrBlank()) return null
    return runCatching {
        val decoded = Uri.decode(argument)
        val bytes = Base64.decode(decoded, Base64.NO_WRAP)
        String(bytes, Charsets.UTF_8)
    }.getOrNull()
}

