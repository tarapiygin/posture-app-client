package com.example.postureapp.data.reports

import android.graphics.BitmapFactory
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ReportAssetUtils {

    fun computeSha256(file: File): String? {
        if (!file.exists()) return null
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }

    fun decodeBounds(file: File): Pair<Int, Int>? {
        if (!file.exists()) return null
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            opts.outWidth to opts.outHeight
        }.getOrNull()
    }
}

