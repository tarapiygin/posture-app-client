package com.example.postureapp.domain.processing

import com.example.postureapp.core.vision.ImageDecoderFacade
import com.example.postureapp.core.vision.MpPoseLandmarker
import com.example.postureapp.domain.landmarks.LandmarkSet
import javax.inject.Inject

class ProcessCroppedImageUseCase @Inject constructor(
    private val decoder: ImageDecoderFacade,
    private val landmarker: MpPoseLandmarker
) {

    suspend operator fun invoke(path: String): LandmarkSet {
        val bitmap = decoder.decode(path)
        return try {
            landmarker.detect(bitmap)
        } finally {
            bitmap.recycle()
        }
    }
}










