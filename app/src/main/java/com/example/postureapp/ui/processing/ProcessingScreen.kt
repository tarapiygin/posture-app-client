package com.example.postureapp.ui.processing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.postureapp.R
import com.example.postureapp.core.designsystem.components.PrimaryButton
import com.example.postureapp.core.designsystem.components.SecondaryButton
import com.example.postureapp.core.designsystem.components.CustomTitleTopBar
import com.example.postureapp.core.navigation.decodePath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    encodedPath: String,
    resultId: String?,
    onBack: () -> Unit,
    onNavigateToEdit: (resultId: String, imagePath: String) -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProcessingViewModel = hiltViewModel()
) {
    val decodedPath = remember(encodedPath) { decodePath(encodedPath).orEmpty() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(decodedPath, resultId) {
        viewModel.start(decodedPath, resultId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProcessingEvent.NavigateToEdit -> onNavigateToEdit(event.resultId, event.imagePath)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CustomTitleTopBar(
                title = stringResource(R.string.processing_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                val errorMessage = uiState.error
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (errorMessage == null) {
                        LoaderContent(progressText = uiState.progressText)
                    } else {
                        ErrorContent(
                            message = errorMessage,
                            onRetry = viewModel::retry,
                            onRetake = onRetake
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoaderContent(progressText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = progressText,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = stringResource(R.string.processing_retry),
            modifier = Modifier.fillMaxWidth(),
            onClick = onRetry
        )
        SecondaryButton(
            text = stringResource(R.string.processing_back_to_camera),
            modifier = Modifier.fillMaxWidth(),
            onClick = onRetake
        )
    }
}

