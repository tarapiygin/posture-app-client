package com.example.postureapp.core.storage

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageSaver {

    private const val DIRECTORY_NAME = "captures"

    fun createCaptureFile(context: Context): File {
        val directory = File(context.filesDir, DIRECTORY_NAME)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(directory, "IMG_${timestamp}.jpg")
    }

    fun createTempCaptureFile(context: Context): File {
        val directory = File(context.cacheDir, DIRECTORY_NAME)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File.createTempFile("TMP_", ".jpg", directory)
    }

    fun writeExifOrientation(file: File, rotationDegrees: Int) {
        runCatching {
            val exif = ExifInterface(file)
            val orientation = when (rotationDegrees) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90
                180 -> ExifInterface.ORIENTATION_ROTATE_180
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> ExifInterface.ORIENTATION_NORMAL
            }
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.saveAttributes()
        }
    }
}

