package com.example.postureapp.data.processing

import com.example.postureapp.domain.landmarks.LandmarkSet
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessingStore @Inject constructor() {

    private val autoResults = ConcurrentHashMap<String, LandmarkSet>()
    private val finalResults = ConcurrentHashMap<String, LandmarkSet>()

    fun put(resultId: String, data: LandmarkSet) {
        autoResults[resultId] = data
    }

    fun get(resultId: String): LandmarkSet? = autoResults[resultId]

    fun remove(resultId: String): LandmarkSet? {
        finalResults.remove(resultId)
        return autoResults.remove(resultId)
    }

    fun getAuto(resultId: String): LandmarkSet? = autoResults[resultId]

    fun hasAuto(resultId: String): Boolean = autoResults.containsKey(resultId)

    fun putFinal(resultId: String, data: LandmarkSet) {
        finalResults[resultId] = data
    }

    fun getFinal(resultId: String): LandmarkSet? = finalResults[resultId]

    fun hasFinal(resultId: String): Boolean = finalResults.containsKey(resultId)

    fun currentFinal(resultId: String): LandmarkSet? {
        return getFinal(resultId) ?: getAuto(resultId)
    }
}






