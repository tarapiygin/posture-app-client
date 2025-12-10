package com.example.postureapp.ui.report

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.postureapp.R
import com.example.postureapp.core.navigation.encodePath
import com.example.postureapp.core.report.Side
import com.example.postureapp.ui.indicators.front.FrontIndicatorsPanel
import com.example.postureapp.ui.indicators.right.RightIndicatorsPanel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportHubScreen(
    startSide: Side,
    onOpenCamera: (Side) -> Unit,
    onOpenCrop: (Side, String, Int) -> Unit,
    onOpenProcessing: (Side, String, String) -> Unit,
    onOpenEdit: (Side, String, String) -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportHubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingGallerySide by remember { mutableStateOf<Side?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val side = pendingGallerySide ?: uiState.activeSide
            scope.launch {
                val path = copyImageToCache(context, uri)
                if (path != null) {
                    viewModel.onOriginalCaptured(side, path)
                    onOpenCrop(side, encodePath(path), 0)
                }
            }
        }
    }

    LaunchedEffect(startSide) {
        viewModel.setStartSide(startSide)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReportHubEvent.OpenCamera -> onOpenCamera(event.side)
                is ReportHubEvent.OpenGallery -> {
                    pendingGallerySide = event.side
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                is ReportHubEvent.NavigateToProcessing -> {
                    onOpenProcessing(
                        event.side,
                        encodePath(event.croppedPath),
                        event.resultId
                    )
                }

                is ReportHubEvent.NavigateToEdit -> {
                    onOpenEdit(
                        event.side,
                        event.resultId,
                        event.imagePath
                    )
                }
            }
        }
    }

    val activeTab = uiState.activeTab
    val activeSide = uiState.activeSide
    val currentSideState = when (activeTab) {
        ReportTab.FRONT -> uiState.front
        ReportTab.RIGHT -> uiState.right
        ReportTab.RESULTS -> if (activeSide == Side.RIGHT) uiState.right else uiState.front
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.report_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.quick_analysis),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* no-op info */ }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {
            ReportTabs(
                active = activeTab,
                onSelect = { tab -> viewModel.selectTab(tab) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                ReportTab.FRONT, ReportTab.RIGHT -> SideContent(
                    state = currentSideState,
                    onTakePhoto = viewModel::openSourceChooser,
                    onStartProcessing = { viewModel.startProcessing(currentSideState.side) },
                    onResetToEdit = { viewModel.editAgain(currentSideState.side) },
                    modifier = Modifier.fillMaxSize()
                )

                ReportTab.RESULTS -> ResultsContent(
                    onNavigateToReports = onNavigateToReports,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (uiState.showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSourceChooser,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.chooser_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.onPickFromCamera() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera_tile),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.chooser_camera),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.onPickFromGallery() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tab_results),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.chooser_gallery),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun SideContent(
    state: SideUiState,
    onTakePhoto: () -> Unit,
    onStartProcessing: () -> Unit,
    onResetToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberBitmap(state.croppedPath)

    when {
        !state.hasImage -> EmptyState(onTakePhoto = onTakePhoto, modifier = modifier)
        state.hasFinal && state.finalLandmarks != null -> {
            if (state.side == Side.RIGHT) {
                RightIndicatorsPanel(
                    imagePath = state.croppedPath.orEmpty(),
                    landmarksFinal = state.finalLandmarks,
                    onResetToEdit = onResetToEdit,
                    modifier = modifier
                )
            } else {
                FrontIndicatorsPanel(
                    imagePath = state.croppedPath.orEmpty(),
                    landmarksFinal = state.finalLandmarks,
                    onResetToEdit = onResetToEdit,
                    modifier = modifier
                )
            }
        }

        else -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                    )
                }
                Surface(
                    onClick = {
                        if (state.hasAuto && state.resultId != null) {
                            onResetToEdit()
                        } else {
                            onStartProcessing()
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(CircleShape)
                ) {
                    Text(
                        text = stringResource(R.string.cta_tap_to_start),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onTakePhoto,
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera_tile),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.cta_take_photo),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ReportTabs(
    active: ReportTab,
    onSelect: (ReportTab) -> Unit
) {
    Surface(shadowElevation = 8.dp, tonalElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabButton(
                label = stringResource(R.string.tab_front),
                icon = R.drawable.ic_tab_front,
                selected = active == ReportTab.FRONT,
                onClick = { onSelect(ReportTab.FRONT) },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                label = stringResource(R.string.tab_right),
                icon = R.drawable.ic_tab_right,
                selected = active == ReportTab.RIGHT,
                onClick = { onSelect(ReportTab.RIGHT) },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                label = stringResource(R.string.tab_results),
                icon = R.drawable.ic_tab_results,
                selected = active == ReportTab.RESULTS,
                onClick = { onSelect(ReportTab.RESULTS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = if (selected) 6.dp else 2.dp,
        shadowElevation = if (selected) 4.dp else 0.dp,
        color = background,
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = content
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = content
            )
        }
    }
}

@Composable
private fun ResultsContent(
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.example.postureapp.ui.results.ResultsTabScreen(
        onNavigateToReports = onNavigateToReports,
        modifier = modifier
    )
}

@Composable
private fun rememberBitmap(path: String?): ImageBitmap? {
    if (path.isNullOrBlank()) return null
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    DisposableBitmapLoader(path) { bitmap = it }
    return bitmap
}

@Composable
private fun DisposableBitmapLoader(
    path: String,
    onLoaded: (ImageBitmap?) -> Unit
) {
    LaunchedEffect(path) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching { android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap() }
                .getOrNull()
        }
        onLoaded(loaded)
    }
}

private suspend fun copyImageToCache(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val input = resolver.openInputStream(uri) ?: return@withContext null
        val tempFile = File(context.cacheDir, "picked_${System.currentTimeMillis()}.jpg")
        input.use { inputStream ->
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
        }
        tempFile.absolutePath
    }
