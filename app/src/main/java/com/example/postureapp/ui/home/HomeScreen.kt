package com.example.postureapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.postureapp.R
import com.example.postureapp.core.designsystem.components.PrimaryButton
import com.example.postureapp.core.designsystem.components.SecondaryButton
import com.example.postureapp.core.permissions.analysisPermissions
import com.example.postureapp.core.permissions.areAllGranted
import com.example.postureapp.core.permissions.openAppSettings
import com.example.postureapp.core.permissions.rememberMultiplePermissionsLauncher

@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onStartAnalysis: () -> Unit,
    onPermissionsDenied: (String?) -> Unit,
    onOpenSettings: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
    onNavigateToAnalysis: () -> Unit
) {
    val context = LocalContext.current
    val rationale = stringResource(R.string.perm_rationale)
    val permissionLauncher = rememberMultiplePermissionsLauncher { result ->
        if (areAllGranted(result)) {
            onNavigateToAnalysis()
        } else {
            onPermissionsDenied(rationale)
        }
    }
    val permissions = remember { analysisPermissions() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        HomeHeroCard(
            description = stringResource(R.string.home_description),
            onStartAnalysis = {
                onStartAnalysis()
            permissionLauncher(permissions)
            }
        )
    }

    if (state.permissionDialogVisible) {
        PermissionDialog(
            message = state.permissionError ?: rationale,
            onOpenSettings = {
                onOpenSettings()
                openAppSettings(context)
            },
            onDismiss = onDismissPermissionDialog
        )
    }
}

@Composable
private fun HomeHeroCard(
    description: String,
    onStartAnalysis: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.home_card_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PrimaryButton(
                text = stringResource(R.string.start_analysis),
                modifier = Modifier.fillMaxWidth(),
                onClick = onStartAnalysis
            )
        }
    }
}

@Composable
private fun PermissionDialog(
    message: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
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
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

