package com.example.postureapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.postureapp.R
import com.example.postureapp.ui.designsystem.components.PrimaryButton
import com.example.postureapp.ui.designsystem.components.SecondaryButton

@Composable
fun AccountSettingsScreen(
    email: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.account_settings_email_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = email.ifBlank { stringResource(R.string.home_value_unknown) },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                text = stringResource(R.string.account_edit_profile),
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                onClick = {}
            )
            PrimaryButton(
                text = stringResource(R.string.account_change_password),
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                onClick = {}
            )
            SecondaryButton(
                text = stringResource(R.string.action_logout),
                modifier = Modifier.fillMaxWidth(),
                onClick = { showLogoutDialog = true }
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = stringResource(R.string.logout_confirm_title)) },
            text = { Text(text = stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.action_logout),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                )
            },
            dismissButton = {
                SecondaryButton(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showLogoutDialog = false }
                )
            }
        )
    }
}




