package com.example.postureapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.postureapp.R
import com.example.postureapp.ui.designsystem.PostureTheme
import com.example.postureapp.ui.designsystem.components.CupertinoTextField
import com.example.postureapp.ui.designsystem.components.InlineError
import com.example.postureapp.ui.designsystem.components.PrimaryButton

@Composable
fun SignUpScreen(
    onSwitchToSignIn: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SignUpEvent.NavigateHome -> onAuthenticated()
            }
        }
    }

    AuthRootScreen(
        selectedTab = AuthTab.SignUp,
        onTabSelected = { tab ->
            if (tab == AuthTab.SignIn) onSwitchToSignIn()
        },
        bannerMessage = uiState.errorMessage,
        showTabs = false,
        titleOverride = stringResource(R.string.signup_title_hero)
    ) {
        SignUpContent(
            state = uiState,
            onEmailChanged = viewModel::onEmailChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onConfirmChanged = viewModel::onConfirmPasswordChanged,
            onSubmit = viewModel::submit,
            onGoogleSignIn = { /* TODO */ },
            onSwitchToSignIn = onSwitchToSignIn
        )
    }
}

@Composable
private fun SignUpContent(
    state: SignUpUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSwitchToSignIn: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CupertinoTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            label = stringResource(R.string.field_email_label),
            placeholder = stringResource(R.string.field_email_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        state.emailError?.let { InlineError(message = it) }

        var showPassword by remember { mutableStateOf(false) }
        CupertinoTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            label = stringResource(R.string.field_password_label),
            placeholder = stringResource(R.string.field_password_placeholder_create),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
        state.passwordError?.let { InlineError(message = it) }

        var showConfirm by remember { mutableStateOf(false) }
        CupertinoTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmChanged,
            label = stringResource(R.string.field_confirm_password_label),
            placeholder = stringResource(R.string.field_confirm_password_placeholder),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(
                        imageVector = if (showConfirm) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
        state.confirmPasswordError?.let { InlineError(message = it) }

        PrimaryButton(
            text = stringResource(R.string.action_create_account),
            enabled = state.canSubmit,
            loading = state.isLoading,
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth()
        )

        AuthDivider(label = stringResource(R.string.or_with))
        GoogleSignInButton(
            label = "",
            onClick = onGoogleSignIn
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.auth_prompt_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(onClick = onSwitchToSignIn) {
                Text(
                    text = stringResource(R.string.action_go_to_sign_in),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(name = "Sign Up")
@Composable
private fun SignUpPreview() {
    PostureTheme {
        AuthRootScreen(
            selectedTab = AuthTab.SignUp,
            onTabSelected = {},
            bannerMessage = "Server error"
        ) {
            SignUpContent(
                state = SignUpUiState(
                    email = "new@example.com",
                    password = "secretPass",
                    confirmPassword = "secretPass"
                ),
                onEmailChanged = {},
                onPasswordChanged = {},
                onConfirmChanged = {},
                onSubmit = {},
                onGoogleSignIn = {},
                onSwitchToSignIn = {}
            )
        }
    }
}

