package com.example.postureapp.core.vision

import android.content.Context
import android.graphics.Bitmap
import com.example.postureapp.domain.landmarks.AnatomicalPoint
import com.example.postureapp.domain.landmarks.Landmark
import com.example.postureapp.domain.landmarks.LandmarkSet
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class MpPoseLandmarker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val landmarkerMutex = Mutex()
    private val lock = Any()
    private var poseLandmarker: PoseLandmarker? = null

    suspend fun detect(bitmap: Bitmap): LandmarkSet = withContext(Dispatchers.Default) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val detector = ensureLandmarker()

        val result = landmarkerMutex.withLock {
            detector.detect(mpImage)
        }

        val rawLandmarks = result.landmarks().firstOrNull()
            ?: throw PoseNotFoundException("Pose not found")

        val posePoints = PoseRawLandmark.entries.associateWith { raw ->
            rawLandmarks.getOrNull(raw.index)?.toPosePoint()
                ?: throw PoseNotFoundException("Pose not found")
        }

        val anatomical = buildAnatomicalLandmarks(posePoints)

        val set = LandmarkSet(
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            points = anatomical
        )

        set.recomputeSynthetic()
    }

    fun close() {
        synchronized(lock) {
            poseLandmarker?.close()
            poseLandmarker = null
        }
    }

    private fun ensureLandmarker(): PoseLandmarker {
        val existing = poseLandmarker
        if (existing != null) return existing
        synchronized(lock) {
            val cached = poseLandmarker
            if (cached != null) return cached
            poseLandmarker = buildLandmarker()
            return poseLandmarker!!
        }
    }

    private fun buildLandmarker(): PoseLandmarker {
        ensureModelAssetPresent()
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET_PATH)
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumPoses(1)
            .build()
        return PoseLandmarker.createFromOptions(context, options)
    }

    private fun ensureModelAssetPresent() {
        try {
            context.assets.open(MODEL_ASSET_PATH).use { /* just check presence */ }
        } catch (io: FileNotFoundException) {
            throw PoseModelMissingException(
                "Missing required model asset at assets/$MODEL_ASSET_PATH",
                io
            )
        } catch (io: IOException) {
            throw PoseModelMissingException(
                "Unable to access model asset at assets/$MODEL_ASSET_PATH",
                io
            )
        }
    }

    private fun buildAnatomicalLandmarks(
        points: Map<PoseRawLandmark, PosePoint>
    ): List<Landmark> {
        fun requirePoint(key: PoseRawLandmark) =
            points[key] ?: throw PoseNotFoundException("Pose not found")

        val leftAnkle = requirePoint(PoseRawLandmark.LEFT_ANKLE)
        val rightAnkle = requirePoint(PoseRawLandmark.RIGHT_ANKLE)
        val leftKnee = requirePoint(PoseRawLandmark.LEFT_KNEE)
        val rightKnee = requirePoint(PoseRawLandmark.RIGHT_KNEE)
        val leftHip = requirePoint(PoseRawLandmark.LEFT_HIP)
        val rightHip = requirePoint(PoseRawLandmark.RIGHT_HIP)
        val leftShoulder = requirePoint(PoseRawLandmark.LEFT_SHOULDER)
        val rightShoulder = requirePoint(PoseRawLandmark.RIGHT_SHOULDER)
        val leftEar = requirePoint(PoseRawLandmark.LEFT_EAR)
        val rightEar = requirePoint(PoseRawLandmark.RIGHT_EAR)

        return listOf(
            leftAnkle.toLandmark(AnatomicalPoint.LEFT_ANKLE),
            rightAnkle.toLandmark(AnatomicalPoint.RIGHT_ANKLE),
            leftKnee.toLandmark(AnatomicalPoint.LEFT_KNEE),
            rightKnee.toLandmark(AnatomicalPoint.RIGHT_KNEE),
            leftHip.toLandmark(AnatomicalPoint.LEFT_HIP),
            rightHip.toLandmark(AnatomicalPoint.RIGHT_HIP),
            leftShoulder.toLandmark(AnatomicalPoint.LEFT_SHOULDER),
            rightShoulder.toLandmark(AnatomicalPoint.RIGHT_SHOULDER),
            leftEar.toLandmark(AnatomicalPoint.LEFT_EAR),
            rightEar.toLandmark(AnatomicalPoint.RIGHT_EAR)
        )
    }

    private fun NormalizedLandmark.toPosePoint(): PosePoint {
        val visibilityValue = this.visibility().orElse(Float.NaN)
        val zValue = this.z()
        return PosePoint(
            x = this.x(),
            y = this.y(),
            z = zValue.takeIf { it.isFinite() },
            visibility = visibilityValue.takeIf { it.isFinite() }
        )
    }

    private data class PosePoint(
        val x: Float,
        val y: Float,
        val z: Float?,
        val visibility: Float?
    )

    private fun PosePoint.toLandmark(point: AnatomicalPoint) = Landmark(
        point = point,
        x = x,
        y = y,
        z = z,
        visibility = visibility,
        editable = point.editable,
        code = point.overlayCode
    )

    companion object {
        private const val MODEL_ASSET_PATH = "models/pose_landmarker_full.task"
    }
}

class PoseNotFoundException(message: String) : Exception(message)

class PoseModelMissingException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)


