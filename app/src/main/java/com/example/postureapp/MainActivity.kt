package com.example.postureapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.postureapp.core.designsystem.PostureTheme
import com.example.postureapp.core.designsystem.SuccessGreen
import com.example.postureapp.ui.BackendStatus
import com.example.postureapp.ui.MainUiState
import com.example.postureapp.ui.MainViewModel
import com.example.postureapp.ui.navigation.AppDestination
import com.example.postureapp.ui.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import com.example.postureapp.R

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PostureTheme {
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
                Column(modifier = Modifier.fillMaxSize()) {
                    BackendStatusBanner(uiState.backendStatus)
                    if (uiState.startDestination == null) {
                        SplashLoading(modifier = Modifier.weight(1f))
                    } else {
                        AppNavHost(
                            startDestination = uiState.startDestination ?: AppDestination.AuthRoot.route,
                            mainViewModel = mainViewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BackendStatusBanner(status: BackendStatus) {
    if (status != BackendStatus.Unknown) {
        val (text, color) = if (status == BackendStatus.Healthy) {
            stringResource(R.string.banner_backend_connected) to SuccessGreen.copy(alpha = 0.16f)
        } else {
            stringResource(R.string.banner_backend_unreachable) to MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.large,
            color = color,
            tonalElevation = 0.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}