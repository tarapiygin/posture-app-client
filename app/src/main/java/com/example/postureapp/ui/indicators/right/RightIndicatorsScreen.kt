package com.example.postureapp.ui.indicators.right

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.postureapp.R

@Composable
fun RightIndicatorsScreen(
    resultId: String,
    imagePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RightIndicatorsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(resultId, imagePath) {
        viewModel.load(resultId, imagePath)
    }

    Surface(modifier = modifier.fillMaxSize()) {
        val landmarks = uiState.landmarks
        when {
            landmarks != null -> RightIndicatorsPanel(
                imagePath = uiState.imagePath,
                landmarksFinal = landmarks,
                onResetToEdit = onBack,
                modifier = Modifier.fillMaxSize()
            )

            uiState.isLoading -> LoadingPlaceholder(onBack = onBack)
            else -> ErrorPlaceholder(onBack = onBack)
        }
    }
}

@Composable
private fun LoadingPlaceholder(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_landmarks_error_missing),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onBack) {
                Text(text = stringResource(R.string.action_back))
            }
        }
    }
}

@Composable
private fun ErrorPlaceholder(onBack: () -> Unit) {
    LoadingPlaceholder(onBack = onBack)
}

