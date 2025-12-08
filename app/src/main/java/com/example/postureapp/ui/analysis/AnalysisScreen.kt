package com.example.postureapp.ui.analysis

import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.postureapp.R
import com.example.postureapp.core.camera.CameraController
import com.example.postureapp.core.designsystem.components.PrimaryButton
import com.example.postureapp.core.designsystem.components.SecondaryButton
import com.example.postureapp.core.navigation.encodePath
import com.example.postureapp.core.permissions.analysisPermissions
import com.example.postureapp.core.permissions.areAllGranted
import com.example.postureapp.core.permissions.openAppSettings
import com.example.postureapp.core.permissions.rememberMultiplePermissionsLauncher
import com.example.postureapp.core.storage.ImageSaver
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AnalysisScreen(
    onBack: () -> Unit,
    onNavigateToCrop: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val inspectionMode = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cameraController = remember(context, inspectionMode) {
        if (inspectionMode) null else CameraController(context)
    }
    val previewView = remember(context, inspectionMode) {
        if (inspectionMode) {
            null
        } else {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }
    }

    val permissions = remember { analysisPermissions() }
    var hasPermissions by remember { mutableStateOf(hasAllPermissions(context, permissions)) }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var flashSupported by remember { mutableStateOf(false) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberMultiplePermissionsLauncher { result ->
        val granted = areAllGranted(result)
        hasPermissions = granted
        showPermissionDialog = !granted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = hasAllPermissions(context, permissions)
                hasPermissions = granted
                if (!granted) {
                    showPermissionDialog = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        viewModel.onAppear()
        onDispose {
            viewModel.onDisappear()
            cameraController?.setTorch(false)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher(permissions)
        }
    }

    LaunchedEffect(hasPermissions, lifecycleOwner, previewView, cameraController) {
        if (hasPermissions && previewView != null && cameraController != null) {
            val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
            cameraController.bindToLifecycle(lifecycleOwner, previewView, rotation)
            flashSupported = cameraController.hasFlash()
            if (!flashSupported) {
                torchEnabled = false
            }
        }
    }

    LaunchedEffect(torchEnabled, flashSupported, cameraController) {
        if (flashSupported) {
            cameraController?.setTorch(torchEnabled)
        }
    }

    var overlayAngles by remember { mutableStateOf(OverlaySample()) }
    val latestAngles by rememberUpdatedState(
        OverlaySample(state.pitch, state.roll, state.aligned)
    )
    LaunchedEffect(Unit) {
        while (isActive) {
            overlayAngles = latestAngles
            delay(16L)
        }
    }

    val captureError = stringResource(R.string.analysis_error_capture)

    LaunchedEffect(state.error) {
        val error = state.error
        if (!error.isNullOrBlank()) {
            snackbarHostState.showSnackbar(error)
            viewModel.onErrorShown()
        }
    }

    fun capture() {
        if (!hasPermissions) {
            showPermissionDialog = true
            return
        }
        if (!viewModel.onShoot()) return

        scope.launch {
            val controller = cameraController
            val view = previewView
            if (controller == null || view == null) {
                viewModel.onCaptureFinished(captureError)
                return@launch
            }

            // rotation сейчас НЕ используем для EXIF, только как запасной вариант
            val rotation = view.display?.rotation ?: Surface.ROTATION_0

            val file = ImageSaver.createTempCaptureFile(context)
            controller.takePicture(file)
                .onSuccess { saved ->
                    // ВАЖНО: НЕ ПЕРЕПИСЫВАЕМ EXIF ОРИЕНТАЦИЮ!
                    viewModel.onCaptureFinished()

                    // rotationDegrees передаём как запасной вариант,
                    // но основной источник правды — EXIF.
                    onNavigateToCrop(
                        encodePath(saved.absolutePath),
                        rotationToDegrees(rotation)
                    )
                }
                .onFailure { throwable ->
                    file.delete()
                    val message = throwable.localizedMessage ?: captureError
                    viewModel.onCaptureFinished(message)
                }
        }
    }

    val cameraPreviewContent: @Composable (Modifier) -> Unit = { previewModifier ->
        if (previewView != null && hasPermissions) {
            AndroidView(
                factory = { previewView },
                modifier = previewModifier
            )
        } else {
            Box(
                modifier = previewModifier
                    .background(Color.Black)
            )
        }
    }

    AnalysisScreenContent(
        state = state,
        overlayAngles = overlayAngles,
        hasPermissions = hasPermissions,
        showPermissionDialog = showPermissionDialog,
        torchVisible = flashSupported,
        torchEnabled = torchEnabled,
        snackbarHostState = snackbarHostState,
        cameraPreviewContent = cameraPreviewContent,
        onBack = onBack,
        onShutter = ::capture,
        onToggleTorch = { torchEnabled = !torchEnabled },
        onRequestPermissions = { permissionLauncher(permissions) },
        onDismissPermissionDialog = {
            showPermissionDialog = false
            onBack()
        },
        onOpenSettings = {
            openAppSettings(context)
            showPermissionDialog = false
        },
        modifier = modifier
    )
}

@Composable
private fun AnalysisScreenContent(
    state: AnalysisUiState,
    overlayAngles: OverlaySample,
    hasPermissions: Boolean,
    showPermissionDialog: Boolean,
    torchVisible: Boolean,
    torchEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    cameraPreviewContent: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
    onShutter: () -> Unit,
    onToggleTorch: () -> Unit,
    onRequestPermissions: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasPermissions) {
                Box(modifier = Modifier.fillMaxSize()) {
                    cameraPreviewContent(Modifier.fillMaxSize())
                    HorizonOverlay(
                        pitch = overlayAngles.pitch,
                        roll = overlayAngles.roll,
                        aligned = overlayAngles.aligned,
                        modifier = Modifier.fillMaxSize(),
                        bottomPadding = if (overlayAngles.aligned) 0.dp else OverlayBottomPadding
                    )
                }
            } else {
                MissingPermissionState(
                    modifier = Modifier.fillMaxSize(),
                    onRequest = onRequestPermissions
                )
            }

            BottomControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                onBack = onBack,
                onShutter = onShutter,
                onToggleTorch = onToggleTorch,
                shutterEnabled = hasPermissions && !state.isCapturing,
                torchEnabled = torchEnabled,
                torchVisible = torchVisible
            )

            if (state.isCapturing) {
                CaptureOverlay()
            }
        }
    }

    if (showPermissionDialog) {
        AnalysisPermissionDialog(
            onOpenSettings = onOpenSettings,
            onDismiss = onDismissPermissionDialog
        )
    }
}

@Composable
private fun BottomControls(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onShutter: () -> Unit,
    onToggleTorch: () -> Unit,
    shutterEnabled: Boolean,
    torchEnabled: Boolean,
    torchVisible: Boolean
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryControlButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            label = stringResource(R.string.action_back),
            onClick = onBack
        )
        ShutterButton(
            enabled = shutterEnabled,
            onClick = onShutter
        )
        if (torchVisible) {
            SecondaryControlButton(
                icon = Icons.Rounded.Bolt,
                label = stringResource(R.string.analysis_torch_label),
                onClick = onToggleTorch,
                selected = torchEnabled,
                showLabel = false
            )
        } else {
            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun SecondaryControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    showLabel: Boolean = true
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
    }
    Surface(
        modifier = Modifier
            .size(width = 120.dp, height = 56.dp),
        shape = RoundedCornerShape(18.dp),
        color = background,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface
            )
            if (showLabel) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .border(
                width = 4.dp,
                color = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
                shape = CircleShape
            )
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (enabled) Color.White else MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
private fun CaptureOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color.White)
            Text(
                text = stringResource(R.string.processing_message),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MissingPermissionState(
    modifier: Modifier = Modifier,
    onRequest: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.perm_rationale),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.start_analysis),
            onClick = onRequest
        )
    }
}

@Composable
private fun AnalysisPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.perm_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.perm_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                PrimaryButton(
                    text = stringResource(R.string.open_settings),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSettings
                )
                SecondaryButton(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                )
            }
        }
    }
}

@Preview(name = "Analysis Screen", showBackground = true)
@Composable
private fun AnalysisScreenPreview() {
    AnalysisScreenContent(
        state = AnalysisUiState(pitch = 2.3f, roll = -1.1f, aligned = false),
        overlayAngles = OverlaySample(pitch = 2.3f, roll = -1.1f, aligned = false),
        hasPermissions = true,
        showPermissionDialog = false,
        torchVisible = true,
        torchEnabled = false,
        snackbarHostState = remember { SnackbarHostState() },
        cameraPreviewContent = { previewModifier ->
            Box(
                modifier = previewModifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            )
        },
        onBack = {},
        onShutter = {},
        onToggleTorch = {},
        onRequestPermissions = {},
        onDismissPermissionDialog = {},
        onOpenSettings = {}
    )
}

private data class OverlaySample(
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val aligned: Boolean = false
)

private val OverlayBottomPadding = 148.dp

private fun rotationToDegrees(rotation: Int): Int = when (rotation) {
    Surface.ROTATION_90 -> 90
    Surface.ROTATION_180 -> 180
    Surface.ROTATION_270 -> 270
    else -> 0
}

private fun hasAllPermissions(
    context: Context,
    permissions: Array<String>
): Boolean = permissions.all { permission ->
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
