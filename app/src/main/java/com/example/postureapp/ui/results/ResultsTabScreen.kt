package com.example.postureapp.ui.results

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.postureapp.R
import com.example.postureapp.core.draw.FrontLevel
import com.example.postureapp.core.draw.RightSegment
import com.example.postureapp.domain.metrics.FrontMetrics
import com.example.postureapp.domain.metrics.right.RightMetrics
import kotlinx.coroutines.launch

@Composable
fun ResultsTabScreen(
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultsTabViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ResultsTabEvent.NavigateToReports -> onNavigateToReports()
                is ResultsTabEvent.Toast -> scope.launch { snackbarHostState.showSnackbar(event.message) }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
                val shareEnabled = state.canShare && !state.sharing
                FloatingActionButton(
                    onClick = { if (shareEnabled) viewModel.onShare() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Rounded.Send, contentDescription = stringResource(R.string.share_report))
                }
                if (state.canSave) {
                    val saveEnabled = !state.saving
                    FloatingActionButton(
                        onClick = { if (saveEnabled) viewModel.onSave() },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(imageVector = Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.save_report))
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.renderData == null -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.not_provided))
            }

            else -> ResultsContent(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                state = state
            )
        }
    }
}

@Composable
private fun ResultsContent(
    state: ResultsTabUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.results_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        item {
            PageCard(title = stringResource(R.string.page_front_overview)) {
                OverviewPage(
                    panel = state.preview?.frontPanel,
                    metrics = state.front?.metrics,
                    placeholder = stringResource(R.string.not_provided)
                )
            }
        }
        item {
            PageCard(title = stringResource(R.string.page_front_details)) {
                TilesPage(
                    tiles = state.preview?.frontTiles?.map { it.level to it.bitmap },
                    metrics = state.front?.metrics,
                    valueFor = { level, metrics -> frontValue(level as FrontLevel, metrics as FrontMetrics?) }
                )
            }
        }
        item {
            PageCard(title = stringResource(R.string.page_right_overview)) {
                OverviewPage(
                    panel = state.preview?.rightPanel,
                    metrics = state.right?.metrics,
                    placeholder = stringResource(R.string.not_provided),
                    isRight = true
                )
            }
        }
        item {
            PageCard(title = stringResource(R.string.page_right_details)) {
                TilesPage(
                    tiles = state.preview?.rightTiles?.map { it.segment to it.bitmap },
                    metrics = state.right?.metrics,
                    valueFor = { segment, metrics -> rightValue(segment as RightSegment, metrics as RightMetrics?) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun PageCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun OverviewPage(
    panel: Bitmap?,
    metrics: Any?,
    placeholder: String,
    isRight: Boolean = false
) {
    if (panel == null || metrics == null) {
        Text(text = placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val imageBitmap = remember(panel) { panel.asImageBitmap() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
        )
        if (!isRight) {
            FrontMetricsTable(metrics as FrontMetrics)
        } else {
            RightMetricsTable(metrics as RightMetrics)
        }
    }
}

@Composable
private fun TilesPage(
    tiles: List<Pair<Any, Bitmap>>?,
    metrics: Any?,
    valueFor: (Any, Any?) -> Float?
) {
    if (tiles.isNullOrEmpty() || metrics == null) {
        Text(text = stringResource(R.string.not_provided), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (key, bitmap) ->
                    val img = remember(bitmap) { bitmap.asImageBitmap() }
                    val value = valueFor(key, metrics)
                    TileCard(
                        title = tileLabel(key),
                        value = value,
                        bitmap = img,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TileCard(
    title: String,
    value: Float?,
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value?.let { String.format("%.1f°", it) } ?: stringResource(R.string.not_provided),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FrontMetricsTable(metrics: FrontMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetricRow(stringResource(R.string.metric_body_dev), metrics.bodyAngleDeg)
        MetricRow(stringResource(R.string.lbl_ears), metrics.levelAngles.find { it.name == "Ears" }?.deviationDeg)
        MetricRow(stringResource(R.string.lbl_shoulders), metrics.levelAngles.find { it.name == "Shoulders" }?.deviationDeg)
        MetricRow(stringResource(R.string.lbl_asis), metrics.levelAngles.find { it.name == "ASIS" }?.deviationDeg)
        MetricRow(stringResource(R.string.lbl_knees), metrics.levelAngles.find { it.name == "Knees" }?.deviationDeg)
        MetricRow(stringResource(R.string.lbl_feet), metrics.levelAngles.find { it.name == "Feet" }?.deviationDeg)
    }
}

@Composable
private fun RightMetricsTable(metrics: RightMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetricRow(stringResource(R.string.metric_body_dev), metrics.bodyAngleDeg)
        MetricRow("CVA", metrics.cvaDeg)
        MetricRow(stringResource(R.string.metric_knee_dev), metrics.segments.find { it.name == "Knee" }?.angleDeg)
        MetricRow(stringResource(R.string.metric_hip_dev), metrics.segments.find { it.name == "Hip" }?.angleDeg)
        MetricRow(stringResource(R.string.metric_shoulder_dev), metrics.segments.find { it.name == "Shoulder" }?.angleDeg)
        MetricRow(stringResource(R.string.metric_ear_dev), metrics.segments.find { it.name == "Ear" }?.angleDeg)
    }
}

@Composable
private fun MetricRow(label: String, value: Float?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value?.let { String.format("%.1f°", it) } ?: stringResource(R.string.not_provided),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun tileLabel(key: Any): String = when (key) {
    is FrontLevel -> when (key) {
        FrontLevel.BODY -> "Body deviation"
        FrontLevel.EARS -> "Head tilt"
        FrontLevel.SHOULDERS -> "Shoulder level"
        FrontLevel.ASIS -> "ASIS level"
        FrontLevel.KNEES -> "Knee level"
        FrontLevel.FEET -> "Feet level"
    }
    is RightSegment -> when (key) {
        RightSegment.CVA -> "CVA"
        RightSegment.KNEE -> "Knee"
        RightSegment.HIP -> "Hip"
        RightSegment.SHOULDER -> "Shoulder"
        RightSegment.EAR -> "Ear"
    }
    else -> ""
}

private fun frontValue(level: FrontLevel, metrics: FrontMetrics?): Float? = when (level) {
    FrontLevel.BODY -> metrics?.bodyAngleDeg
    FrontLevel.EARS -> metrics?.levelAngles?.find { it.name == "Ears" }?.deviationDeg
    FrontLevel.SHOULDERS -> metrics?.levelAngles?.find { it.name == "Shoulders" }?.deviationDeg
    FrontLevel.ASIS -> metrics?.levelAngles?.find { it.name == "ASIS" }?.deviationDeg
    FrontLevel.KNEES -> metrics?.levelAngles?.find { it.name == "Knees" }?.deviationDeg
    FrontLevel.FEET -> metrics?.levelAngles?.find { it.name == "Feet" }?.deviationDeg
}

private fun rightValue(segment: RightSegment, metrics: RightMetrics?): Float? = when (segment) {
    RightSegment.CVA -> metrics?.cvaDeg
    RightSegment.KNEE -> metrics?.segments?.find { it.name == "Knee" }?.angleDeg
    RightSegment.HIP -> metrics?.segments?.find { it.name == "Hip" }?.angleDeg
    RightSegment.SHOULDER -> metrics?.segments?.find { it.name == "Shoulder" }?.angleDeg
    RightSegment.EAR -> metrics?.segments?.find { it.name == "Ear" }?.angleDeg
}

