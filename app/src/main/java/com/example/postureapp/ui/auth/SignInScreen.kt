package com.example.postureapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.postureapp.R
import com.example.postureapp.ui.designsystem.PostureTheme
import com.example.postureapp.ui.designsystem.components.CupertinoTextField
import com.example.postureapp.ui.designsystem.components.InlineError
import com.example.postureapp.ui.designsystem.components.PrimaryButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Divider

@Composable
fun SignInScreen(
    onSwitchToSignUp: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SignInEvent.NavigateHome -> onAuthenticated()
            }
        }
    }

    AuthRootScreen(
        selectedTab = AuthTab.SignIn,
        onTabSelected = { tab ->
            if (tab == AuthTab.SignUp) onSwitchToSignUp()
        },
        bannerMessage = uiState.errorMessage,
        showTabs = false,
        titleOverride = stringResource(R.string.signin_title_hero)
    ) {
        SignInContent(
            state = uiState,
            onEmailChanged = viewModel::onEmailChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onSubmit = viewModel::submit,
            onForgotPassword = { /* TODO */ },
            onGoogleSignIn = { /* TODO */ },
            onSwitchToSignUp = onSwitchToSignUp
        )
    }
}

@Composable
private fun SignInContent(
    state: SignInUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSwitchToSignUp: () -> Unit
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
            placeholder = stringResource(R.string.field_password_placeholder_mask),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
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

        PrimaryButton(
            text = stringResource(R.string.action_sign_in),
            enabled = state.canSubmit,
            loading = state.isLoading,
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(
            onClick = onForgotPassword,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = stringResource(R.string.action_forgot_password),
                color = MaterialTheme.colorScheme.primary
            )
        }

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
                text = stringResource(R.string.auth_prompt_no_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(onClick = onSwitchToSignUp) {
                Text(
                    text = stringResource(R.string.action_go_to_sign_up),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(name = "Sign In")
@Composable
private fun SignInPreview() {
    PostureTheme {
        AuthRootScreen(
            selectedTab = AuthTab.SignIn,
            onTabSelected = {},
            bannerMessage = null
        ) {
        SignInContent(
            state = SignInUiState(email = "clinician@example.com", password = "secret"),
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmit = {},
            onForgotPassword = {},
            onGoogleSignIn = {},
            onSwitchToSignUp = {}
        )
        }
    }
}
