package com.example.postureapp.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.postureapp.core.report.Side
import com.example.postureapp.data.processing.ProcessingStore
import com.example.postureapp.data.report.ReportCoordinator
import com.example.postureapp.data.report.ReportSession
import com.example.postureapp.data.report.SideState
import com.example.postureapp.domain.landmarks.LandmarkSet
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ReportHubViewModel @Inject constructor(
    private val coordinator: ReportCoordinator,
    private val processingStore: ProcessingStore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val activeSide = MutableStateFlow(savedStateHandle.restoreSide())
    private val activeTab: MutableStateFlow<ReportTab> = MutableStateFlow(ReportTab.FRONT)
    private val showSourceSheet = MutableStateFlow(false)

    private val _events = Channel<ReportHubEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        coordinator.startNewIfNeeded()
    }

    val uiState: StateFlow<ReportHubUiState> = combine(
        coordinator.sessionFlow,
        activeSide,
        activeTab,
        showSourceSheet
    ) { session, side, tab, sheetVisible ->
        savedStateHandle[KEY_SIDE] = side.name
        ReportHubUiState(
            activeSide = side,
            activeTab = tab,
            session = session,
            front = session.front.toUiState(),
            right = session.right.toUiState(),
            showSourceSheet = sheetVisible
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReportHubUiState()
    )

    fun setStartSide(side: Side) {
        activeSide.value = side
        activeTab.value = ReportTab.fromSide(side)
    }

    fun switchSide(side: Side) {
        activeSide.value = side
        activeTab.value = ReportTab.fromSide(side)
        showSourceSheet.value = false
    }

    fun selectTab(tab: ReportTab) {
        activeTab.value = tab
        if (tab is ReportTab.SideTab) {
            activeSide.value = tab.side
        }
    }

    fun openSourceChooser() {
        showSourceSheet.value = true
    }

    fun dismissSourceChooser() {
        showSourceSheet.value = false
    }

    fun onPickFromCamera() {
        showSourceSheet.value = false
        viewModelScope.launch {
            _events.send(ReportHubEvent.OpenCamera(activeSide.value))
        }
    }

    fun onPickFromGallery() {
        showSourceSheet.value = false
        viewModelScope.launch {
            _events.send(ReportHubEvent.OpenGallery(activeSide.value))
        }
    }

    fun onOriginalCaptured(side: Side, path: String) {
        coordinator.setOriginal(side, path)
        activeSide.value = side
    }

    fun onCropped(side: Side, path: String) {
        coordinator.setCropped(side, path)
        activeSide.value = side
    }

    fun startProcessing(side: Side) {
        val cropped = coordinator.currentSideState(side).croppedPath ?: return
        val resultId = coordinator.ensureResultId(side)
        viewModelScope.launch {
            _events.send(ReportHubEvent.NavigateToProcessing(side, cropped, resultId))
        }
    }

    fun onAutoReady(side: Side) {
        coordinator.markAutoReady(side)
    }

    fun onFinalized(side: Side) {
        coordinator.markFinalReady(side)
    }

    fun editAgain(side: Side? = null) {
        val target = side ?: activeSide.value
        val state = coordinator.currentSideState(target)
        val resultId = state.resultId ?: return
        val path = state.croppedPath ?: return
        viewModelScope.launch {
            _events.send(ReportHubEvent.NavigateToEdit(target, resultId, path))
        }
    }

    private fun SideState.toUiState(): SideUiState {
        val auto = resultId?.let { processingStore.getAuto(it) }
        val final = resultId?.let { processingStore.getFinal(it) }
        val hasAutoFlag = hasAuto && resultId?.let { processingStore.hasAuto(it) } == true
        val hasFinalFlag = hasFinal && resultId?.let { processingStore.hasFinal(it) } == true
        return SideUiState(
            side = side,
            originalPath = originalPath,
            croppedPath = croppedPath,
            resultId = resultId,
            hasAuto = hasAutoFlag || auto != null,
            hasFinal = hasFinalFlag || final != null,
            autoLandmarks = auto,
            finalLandmarks = final
        )
    }

    private fun SavedStateHandle.restoreSide(): Side {
        val value = get<String>(KEY_SIDE)
        return value?.let { runCatching { Side.valueOf(it) }.getOrNull() } ?: Side.FRONT
    }

    companion object {
        private const val KEY_SIDE = "report_active_side"
    }
}

data class ReportHubUiState(
    val activeSide: Side = Side.FRONT,
    val activeTab: ReportTab = ReportTab.FRONT,
    val session: ReportSession = ReportSession(id = ""),
    val front: SideUiState = SideUiState.empty(Side.FRONT),
    val right: SideUiState = SideUiState.empty(Side.RIGHT),
    val showSourceSheet: Boolean = false
)

data class SideUiState(
    val side: Side,
    val originalPath: String? = null,
    val croppedPath: String? = null,
    val resultId: String? = null,
    val hasAuto: Boolean = false,
    val hasFinal: Boolean = false,
    val autoLandmarks: LandmarkSet? = null,
    val finalLandmarks: LandmarkSet? = null
) {
    val hasImage: Boolean get() = !croppedPath.isNullOrBlank()

    companion object {
        fun empty(side: Side) = SideUiState(side = side)
    }
}

sealed interface ReportTab {
    data object FRONT : ReportTab, SideTab { override val side: Side = Side.FRONT }
    data object RIGHT : ReportTab, SideTab { override val side: Side = Side.RIGHT }
    data object RESULTS : ReportTab

    interface SideTab {
        val side: Side
    }

    companion object {
        fun fromSide(side: Side): ReportTab = if (side == Side.FRONT) FRONT else RIGHT
    }
}

sealed interface ReportHubEvent {
    data class OpenCamera(val side: Side) : ReportHubEvent
    data class OpenGallery(val side: Side) : ReportHubEvent
    data class NavigateToProcessing(val side: Side, val croppedPath: String, val resultId: String) :
        ReportHubEvent

    data class NavigateToEdit(val side: Side, val resultId: String, val imagePath: String) :
        ReportHubEvent
}
