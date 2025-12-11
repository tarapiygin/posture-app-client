package com.example.postureapp.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.postureapp.core.analysis.Side
import com.example.postureapp.data.auth.AuthState
import com.example.postureapp.ui.MainViewModel
import com.example.postureapp.ui.capture.photo.PhotoCaptureScreen
import com.example.postureapp.ui.auth.SignInScreen
import com.example.postureapp.ui.auth.SignUpScreen
import com.example.postureapp.ui.capture.crop.CropScreen
import com.example.postureapp.ui.analysis.indicators.edit.EditLandmarksScreen
import com.example.postureapp.ui.feedback.FeedbackScreen
import com.example.postureapp.ui.home.HomeScreen
import com.example.postureapp.ui.home.HomeViewModel
import com.example.postureapp.ui.analysis.indicators.front.FrontIndicatorsScreen
import com.example.postureapp.ui.analysis.indicators.right.RightIndicatorsScreen
import com.example.postureapp.ui.main.MainScaffold
import com.example.postureapp.ui.analysis.processing.ProcessingScreen
import com.example.postureapp.ui.analysis.AnalysisScreen
import com.example.postureapp.ui.reports.ReportsScreen
import com.example.postureapp.ui.reports.ReportViewerScreen
import com.example.postureapp.ui.settings.AccountSettingsScreen

@Composable
fun AppNavHost(
    startDestination: String,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val authState = mainViewModel.authState.collectAsStateWithLifecycle().value
    val currentUser = mainViewModel.currentUser.collectAsStateWithLifecycle().value

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Authenticated -> {
                val destination = navController.currentDestination?.route
                if (
                    destination == AppDestination.SignIn.route ||
                    destination == AppDestination.SignUp.route ||
                    destination == AppDestination.AuthRoot.route
                ) {
                    navController.navigate(MainGraphRoute) {
                        popUpTo(AppDestination.AuthRoot.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }

            AuthState.SignedOut -> {
                navController.navigate(AppDestination.AuthRoot.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        navigation(
            route = AppDestination.AuthRoot.route,
            startDestination = AppDestination.SignIn.route
        ) {
            composable(AppDestination.SignIn.route) {
                SignInScreen(
                    onSwitchToSignUp = {
                        navController.navigate(AppDestination.SignUp.route) {
                            launchSingleTop = true
                        }
                    },
                    onAuthenticated = {
                        navController.navigate(MainGraphRoute) {
                            popUpTo(AppDestination.AuthRoot.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppDestination.SignUp.route) {
                SignUpScreen(
                    onSwitchToSignIn = {
                        navController.navigate(AppDestination.SignIn.route) {
                            launchSingleTop = true
                        }
                    },
                    onAuthenticated = {
                        navController.navigate(MainGraphRoute) {
                            popUpTo(AppDestination.AuthRoot.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        navigation(
            route = MainGraphRoute,
            startDestination = Destinations.Home.route
        ) {
            composable(Destinations.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                val homeState = homeViewModel.uiState.collectAsStateWithLifecycle().value
                MainScaffold(
                    currentDestination = Destinations.Home,
                    navController = navController
                ) { padding ->
                    HomeScreen(
                        state = homeState,
                        modifier = Modifier.padding(padding),
                        onStartAnalysis = homeViewModel::onStartAnalysis,
                        onPermissionsDenied = homeViewModel::onPermissionsDenied,
                        onOpenSettings = homeViewModel::onOpenSettings,
                        onDismissPermissionDialog = homeViewModel::onDismissPermissionDialog,
                        onNavigateToAnalysis = {
                            navController.navigate(Destinations.ReportHub.create(Side.FRONT).route)
                        }
                    )
                }
            }

            composable(Destinations.AccountSettings.route) {
                MainScaffold(
                    currentDestination = Destinations.AccountSettings,
                    navController = navController
                ) { padding ->
                    AccountSettingsScreen(
                        email = currentUser?.email.orEmpty(),
                        onLogout = mainViewModel::logout,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(Destinations.Reports.route) {
                MainScaffold(
                    currentDestination = Destinations.Reports,
                    navController = navController
                ) { padding ->
                    ReportsScreen(
                        onOpenReport = { id ->
                            navController.navigate(Destinations.ReportViewer.create(id).route)
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(Destinations.Feedback.route) {
                MainScaffold(
                    currentDestination = Destinations.Feedback,
                    navController = navController
                ) { padding ->
                    FeedbackScreen(
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }

        composable(
            route = Destinations.ReportHub.routePattern,
            arguments = listOf(
                navArgument("side") { type = NavType.StringType; defaultValue = Side.FRONT.name }
            )
        ) { backStackEntry ->
            val sideArg = backStackEntry.arguments?.getString("side") ?: Side.FRONT.name
            val startSide = runCatching { Side.valueOf(sideArg) }.getOrDefault(Side.FRONT)
            val savedStateHandle = backStackEntry.savedStateHandle
            val hubViewModel: com.example.postureapp.ui.analysis.AnalysisViewModel = hiltViewModel(backStackEntry)

            val croppedPath by savedStateHandle.getStateFlow("cropped_path", "").collectAsStateWithLifecycle()
            val croppedSideRaw by savedStateHandle.getStateFlow("cropped_side", "").collectAsStateWithLifecycle()
            val finalizedSideRaw by savedStateHandle.getStateFlow("finalized_side", "").collectAsStateWithLifecycle()
            val autoSideRaw by savedStateHandle.getStateFlow("auto_side", "").collectAsStateWithLifecycle()

            LaunchedEffect(startSide) { hubViewModel.setStartSide(startSide) }

            LaunchedEffect(croppedPath, croppedSideRaw) {
                val side = runCatching { Side.valueOf(croppedSideRaw) }.getOrNull()
                if (croppedPath.isNotBlank() && side != null) {
                    hubViewModel.onCropped(side, croppedPath)
                    savedStateHandle["cropped_path"] = ""
                    savedStateHandle["cropped_side"] = ""
                }
            }
            LaunchedEffect(finalizedSideRaw) {
                val side = runCatching { Side.valueOf(finalizedSideRaw) }.getOrNull()
                if (!finalizedSideRaw.isNullOrEmpty() && side != null) {
                    hubViewModel.onFinalized(side)
                    savedStateHandle["finalized_side"] = ""
                }
            }
            LaunchedEffect(autoSideRaw) {
                val side = runCatching { Side.valueOf(autoSideRaw) }.getOrNull()
                if (!autoSideRaw.isNullOrEmpty() && side != null) {
                    hubViewModel.onAutoReady(side)
                    savedStateHandle["auto_side"] = ""
                }
            }
            AnalysisScreen(
                startSide = startSide,
                onOpenCamera = { side ->
                    navController.navigate(Destinations.Analysis.create(side).route)
                },
                onOpenCrop = { side, encodedPath, rotation ->
                    navController.navigate(Destinations.Crop(encodedPath, rotation, side.name).route)
                },
                onOpenProcessing = { side, encodedPath, resultId ->
                    navController.navigate(
                        Destinations.Processing(encodedPath, resultId, side.name).route
                    )
                },
                onOpenEdit = { side, resultId, imagePath ->
                    navController.navigate(
                        Destinations.EditLandmarks.create(resultId, imagePath, side).route
                    )
                },
                onNavigateToReports = {
                    navController.navigate(Destinations.Reports.route) {
                        popUpTo(MainGraphRoute) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
            )
        }

        composable(
            route = Destinations.Analysis.routePattern,
            arguments = listOf(
                navArgument("side") { type = NavType.StringType; defaultValue = Side.FRONT.name }
            )
        ) { backStackEntry ->
            val sideArg = backStackEntry.arguments?.getString("side") ?: Side.FRONT.name
            val side = runCatching { Side.valueOf(sideArg) }.getOrDefault(Side.FRONT)
            PhotoCaptureScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCrop = { encodedPath, rotation ->
                    navController.navigate(Destinations.Crop(encodedPath, rotation, side.name).route)
                }
            )
        }

        composable(
            route = Destinations.Crop.routePattern,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; nullable = false },
                navArgument("rotation") { type = NavType.IntType },
                navArgument("side") { type = NavType.StringType; defaultValue = Side.FRONT.name }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path").orEmpty()
            val rotation = backStackEntry.arguments?.getInt("rotation") ?: 0
            val sideArg = backStackEntry.arguments?.getString("side") ?: Side.FRONT.name
            val side = runCatching { Side.valueOf(sideArg) }.getOrDefault(Side.FRONT)
            CropScreen(
                encodedPath = encodedPath,
                rotationDegrees = rotation,
                onBack = { navController.popBackStack() },
                onCropped = { finalPath ->
                    val hubRoute = Destinations.ReportHub.create(side).route
                    var hubEntry = runCatching { navController.getBackStackEntry(hubRoute) }.getOrNull()
                    if (hubEntry == null) {
                        navController.navigate(hubRoute) {
                            popUpTo(Destinations.ReportHub.create(side).route) { inclusive = false }
                            launchSingleTop = true
                        }
                        hubEntry = runCatching { navController.getBackStackEntry(hubRoute) }.getOrNull()
                    }
                    hubEntry?.savedStateHandle?.apply {
                        set("cropped_path", finalPath)
                        set("cropped_side", side.name)
                    }
                    navController.popBackStack(hubRoute, false)
                }
            )
        }

        composable(
            route = Destinations.Processing.routePattern,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; nullable = false },
                navArgument("rid") { type = NavType.StringType; defaultValue = "" },
                navArgument("side") { type = NavType.StringType; defaultValue = Side.FRONT.name }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path").orEmpty()
            val ridArg = backStackEntry.arguments?.getString("rid").orEmpty()
            val sideArg = backStackEntry.arguments?.getString("side") ?: Side.FRONT.name
            val side = runCatching { Side.valueOf(sideArg) }.getOrDefault(Side.FRONT)
            ProcessingScreen(
                encodedPath = encodedPath,
                resultId = ridArg.takeIf { it.isNotBlank() },
                onBack = { navController.popBackStack() },
                onNavigateToEdit = { resultId, imagePath ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("auto_side", side.name)
                    navController.navigate(
                        Destinations.EditLandmarks.create(resultId, imagePath, side).route
                    ) {
                        launchSingleTop = true
                    }
                },
                onRetake = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Destinations.EditLandmarks.routePattern,
            arguments = listOf(
                navArgument("rid") { type = NavType.StringType; nullable = false },
                navArgument("path") { type = NavType.StringType; nullable = false },
                navArgument("side") { type = NavType.StringType; defaultValue = Side.FRONT.name }
            )
        ) { backStackEntry ->
            val resultId = backStackEntry.arguments?.getString("rid").orEmpty()
            val rawPath = backStackEntry.arguments?.getString("path").orEmpty()
            val decodedPath = Uri.decode(rawPath)
            val sideArg = backStackEntry.arguments?.getString("side") ?: Side.FRONT.name
            val side = runCatching { Side.valueOf(sideArg) }.getOrDefault(Side.FRONT)
            EditLandmarksScreen(
                resultId = resultId,
                imagePath = decodedPath,
                side = side,
                onBack = { navController.popBackStack() },
                onNavigateToIndicators = { rid, path ->
                    val hubRoute = Destinations.ReportHub.create(side).route
                    val hubEntry = runCatching { navController.getBackStackEntry(hubRoute) }.getOrNull()
                    hubEntry?.savedStateHandle?.set("finalized_side", side.name)
                    val popped = navController.popBackStack(hubRoute, false)
                    if (!popped) {
                        navController.navigate(hubRoute) {
                            popUpTo(Destinations.ReportHub.create(side).route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = Destinations.FrontIndicators.routePattern,
            arguments = listOf(
                navArgument("rid") {
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("path") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val encodedRid = backStackEntry.arguments?.getString("rid").orEmpty()
            val encodedPath = backStackEntry.arguments?.getString("path").orEmpty()
            val resultId = Uri.decode(encodedRid)
            val imagePath = Uri.decode(encodedPath)
            FrontIndicatorsScreen(
                resultId = resultId,
                imagePath = imagePath,
                onResetToEdit = { navController.popBackStack() },
            )
        }

        composable(
            route = Destinations.RightSideIndicators.routePattern,
            arguments = listOf(
                navArgument("rid") {
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("path") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val rid = backStackEntry.arguments?.getString("rid").orEmpty()
            val path = backStackEntry.arguments?.getString("path").orEmpty()
            RightIndicatorsScreen(
                resultId = Uri.decode(rid),
                imagePath = Uri.decode(path),
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Destinations.ReportViewer.routePattern,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            ReportViewerScreen(
                reportId = id,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }
    }
}
