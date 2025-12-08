package com.example.postureapp.core.camera

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    suspend fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        targetRotation: Int
    ) {
        val cameraProvider = provideCameraProvider()
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(targetRotation)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(90)
            .setTargetRotation(targetRotation)
            .build()

        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
        this.imageCapture = imageCapture
    }

    suspend fun takePicture(outputFile: File): Result<File> {
        val capture = imageCapture ?: return Result.failure(IllegalStateException("Camera not bound"))
        return suspendCancellableCoroutine { continuation ->
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            capture.takePicture(
                outputOptions,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        if (continuation.isActive) {
                            continuation.resume(Result.success(outputFile))
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(exception))
                        }
                    }
                }
            )
        }
    }

    fun setTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    fun hasFlash(): Boolean = camera?.cameraInfo?.hasFlashUnit() == true

    private suspend fun provideCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            cameraProviderFuture.addListener(
                {
                    runCatching { cameraProviderFuture.get() }
                        .onSuccess { provider -> continuation.resume(provider) }
                        .onFailure { exception ->
                            if (continuation.isActive) {
                                continuation.resumeWithException(exception)
                            }
                        }
                },
                mainExecutor
            )
            continuation.invokeOnCancellation {
                // Intentionally left blank. CameraX manages its own future lifecycle.
            }
        }
}

