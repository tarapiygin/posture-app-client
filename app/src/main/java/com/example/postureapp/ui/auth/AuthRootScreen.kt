package com.example.postureapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.postureapp.R
import com.example.postureapp.ui.designsystem.PostureShapes
import com.example.postureapp.ui.designsystem.components.CustomTitleTopBar

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

enum class AuthTab(@StringRes val labelRes: Int) {
    SignIn(R.string.auth_tab_sign_in),
    SignUp(R.string.auth_tab_sign_up)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthRootScreen(
    selectedTab: AuthTab,
    onTabSelected: (AuthTab) -> Unit,
    bannerMessage: String?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    showTabs: Boolean = true,
    titleOverride: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val title = titleOverride ?: when (selectedTab) {
        AuthTab.SignIn -> stringResource(R.string.auth_title_welcome_back)
        AuthTab.SignUp -> stringResource(R.string.auth_title_create_account)
    }
    val cardColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .navigationBarsPadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (showTabs) {
                SegmentedControl(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
            if (bannerMessage != null) {
                Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    shape = PostureShapes.medium,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = bannerMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Surface(
                shape = PostureShapes.large,
                tonalElevation = 2.dp,
                color = cardColor,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    selectedTab: AuthTab,
    onTabSelected: (AuthTab) -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val unselectedText = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = shape,
        color = trackColor,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AuthTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(shape)
                        .clickable { if (!isSelected) onTabSelected(tab) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) Color.White else unselectedText,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) Color.White else unselectedText
                        )
                    }
                }
                if (tab == AuthTab.SignIn) {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }
    }
}

