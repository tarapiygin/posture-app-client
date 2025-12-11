package com.example.postureapp.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.example.postureapp.ui.designsystem.components.CustomTitleTopBar
import com.example.postureapp.ui.navigation.BottomDestinations
import com.example.postureapp.ui.navigation.Destinations
import com.example.postureapp.ui.navigation.MainGraphRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    currentDestination: Destinations,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopBar(
                title = stringResource(id = currentDestination.titleRes),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            MainBottomNavigation(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    if (destination.route != currentDestination.route) {
                        navController.navigate(destination.route) {
                            popUpTo(MainGraphRoute) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

@Composable
private fun MainBottomNavigation(
    currentDestination: Destinations,
    onDestinationSelected: (Destinations) -> Unit
) {
    NavigationBar {
        BottomDestinations.forEach { destination ->
            NavigationBarItem(
                selected = destination.route == currentDestination.route,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes)
                    )
                },
                label = {
                    Text(text = stringResource(destination.labelRes))
                },
                alwaysShowLabel = true
            )
        }
    }
}




