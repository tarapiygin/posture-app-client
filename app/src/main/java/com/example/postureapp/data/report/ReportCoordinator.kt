package com.example.postureapp.data.report

import com.example.postureapp.core.report.Side
import com.example.postureapp.data.processing.ProcessingStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Singleton
class ReportCoordinator @Inject constructor(
    private val processingStore: ProcessingStore
) {

    private val _sessionFlow = MutableStateFlow(newSession())
    val sessionFlow: StateFlow<ReportSession> = _sessionFlow

    fun startNewIfNeeded(): String {
        val current = _sessionFlow.value
        if (current.id.isNotBlank()) return current.id
        val fresh = newSession()
        _sessionFlow.value = fresh
        return fresh.id
    }

    fun setOriginal(side: Side, path: String) {
        _sessionFlow.update { session ->
            session.updateSide(side) {
                copy(
                    originalPath = path,
                    croppedPath = null,
                    resultId = null,
                    hasAuto = false,
                    hasFinal = false
                )
            }
        }
    }

    fun setCropped(side: Side, path: String) {
        _sessionFlow.update { session ->
            session.updateSide(side) {
                copy(
                    croppedPath = path,
                    hasAuto = false,
                    hasFinal = false
                )
            }
        }
    }

    fun ensureResultId(side: Side): String {
        var existing: String? = null
        _sessionFlow.update { session ->
            val current = session.currentSide(side)
            val rid = current.resultId ?: UUID.randomUUID().toString()
            existing = rid
            session.updateSide(side) {
                copy(resultId = rid)
            }
        }
        return existing ?: UUID.randomUUID().toString()
    }

    fun markAutoReady(side: Side) {
        _sessionFlow.update { session ->
            session.updateSide(side) {
                copy(hasAuto = true)
            }
        }
    }

    fun markFinalReady(side: Side) {
        _sessionFlow.update { session ->
            session.updateSide(side) {
                copy(hasFinal = true, hasAuto = true)
            }
        }
    }

    fun currentSideState(side: Side): SideState = _sessionFlow.value.currentSide(side)

    fun reset(): String {
        val fresh = newSession()
        _sessionFlow.value = fresh
        return fresh.id
    }

    private fun ReportSession.updateSide(
        side: Side,
        block: SideState.() -> SideState
    ): ReportSession = when (side) {
        Side.FRONT -> copy(front = block(front))
        Side.RIGHT -> copy(right = block(right))
    }

    private fun ReportSession.currentSide(side: Side): SideState = when (side) {
        Side.FRONT -> front
        Side.RIGHT -> right
    }

    private fun newSession(): ReportSession = ReportSession(UUID.randomUUID().toString())
}



