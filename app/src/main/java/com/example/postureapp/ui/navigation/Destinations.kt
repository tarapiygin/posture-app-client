package com.example.postureapp.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.postureapp.R
import com.example.postureapp.core.report.Side

const val MainGraphRoute = "main"

sealed class Destinations(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Home : Destinations(
        route = "home",
        titleRes = R.string.home_title,
        labelRes = R.string.bottom_home,
        icon = Icons.Rounded.Apps
    )

    data object AccountSettings : Destinations(
        route = "account_settings",
        titleRes = R.string.account_settings_title,
        labelRes = R.string.bottom_settings,
        icon = Icons.Rounded.Person
    )

    data object Reports : Destinations(
        route = "reports",
        titleRes = R.string.reports_title,
        labelRes = R.string.bottom_reports,
        icon = Icons.Rounded.Description
    )

    data object Feedback : Destinations(
        route = "feedback",
        titleRes = R.string.feedback_title,
        labelRes = R.string.bottom_feedback,
        icon = Icons.Rounded.HeadsetMic
    )

    data class ReportHub(
        private val side: String = Side.FRONT.name
    ) : Destinations(
        route = "report_hub?side=$side",
        titleRes = R.string.report_title,
        labelRes = R.string.report_title,
        icon = Icons.Rounded.AutoAwesome
    ) {
        companion object {
            const val routePattern = "report_hub?side={side}"

            fun create(side: Side): ReportHub = ReportHub(side.name)
        }
    }

    data class Analysis(
        private val side: String = Side.FRONT.name
    ) : Destinations(
        route = "analysis?side=$side",
        titleRes = R.string.analysis_title,
        labelRes = R.string.analysis_title,
        icon = Icons.Rounded.AutoAwesome
    ) {
        companion object {
            const val routePattern = "analysis?side={side}"

            fun create(side: Side): Analysis = Analysis(side.name)
        }
    }

    data class Processing(
        private val encodedPath: String,
        private val encodedResultId: String = "",
        private val side: String = Side.FRONT.name
    ) : Destinations(
        route = "processing?path=$encodedPath&rid=$encodedResultId&side=$side",
        titleRes = R.string.processing_title,
        labelRes = R.string.processing_title,
        icon = Icons.Rounded.AutoAwesome
    ) {
        companion object {
            const val routePattern = "processing?path={path}&rid={rid}&side={side}"
        }
    }

    data class Crop(
        private val encodedPath: String,
        private val rotation: Int,
        private val side: String = Side.FRONT.name
    ) : Destinations(
        route = "crop?path=$encodedPath&rotation=$rotation&side=$side",
        titleRes = R.string.crop_title,
        labelRes = R.string.crop_title,
        icon = Icons.Rounded.AutoAwesome
    ) {
        companion object {
            const val routePattern = "crop?path={path}&rotation={rotation}&side={side}"
        }
    }

    data class EditLandmarks(
        private val encodedResultId: String,
        private val encodedPath: String,
        private val side: String
    ) : Destinations(
        route = "edit_landmarks?rid=$encodedResultId&path=$encodedPath&side=$side",
        titleRes = R.string.edit_landmarks_title,
        labelRes = R.string.edit_landmarks_title,
        icon = Icons.Rounded.AutoAwesome
    ) {
        companion object {
            const val routePattern = "edit_landmarks?rid={rid}&path={path}&side={side}"

            fun create(resultId: String, imagePath: String, side: Side): EditLandmarks {
                val rid = Uri.encode(resultId)
                val path = Uri.encode(imagePath)
                return EditLandmarks(rid, path, side.name)
            }
        }
    }

    data class FrontIndicators(
        private val encodedResultId: String,
        private val encodedPath: String
    ) : Destinations(
        route = "front_indicators?rid=$encodedResultId&path=$encodedPath",
        titleRes = R.string.front_view_title,
        labelRes = R.string.front_view_title,
        icon = Icons.Rounded.AutoAwesome
    ) {
        companion object {
            const val routePattern = "front_indicators?rid={rid}&path={path}"

            fun create(resultId: String, imagePath: String): FrontIndicators {
                val rid = Uri.encode(resultId)
                val path = Uri.encode(imagePath)
                return FrontIndicators(rid, path)
            }
        }
    }

    data class FrontResultsList(
        private val encodedResultId: String
    ) : Destinations(
        route = "front_results?rid=$encodedResultId",
        titleRes = R.string.tab_results,
        labelRes = R.string.tab_results,
        icon = Icons.Rounded.AutoAwesome
    ) {
        companion object {
            const val routePattern = "front_results?rid={rid}"

            fun create(resultId: String): FrontResultsList {
                val rid = Uri.encode(resultId)
                return FrontResultsList(rid)
            }
        }
    }

    data class RightSideIndicators(
        private val encodedResultId: String,
        private val encodedPath: String
    ) : Destinations(
        route = "right_indicators?rid=$encodedResultId&path=$encodedPath",
        titleRes = R.string.tab_right_side,
        labelRes = R.string.tab_right_side,
        icon = Icons.Rounded.AutoAwesome
    ) {
        companion object {
            const val routePattern = "right_indicators?rid={rid}&path={path}"

            fun create(resultId: String, imagePath: String): RightSideIndicators {
                val rid = Uri.encode(resultId)
                val path = Uri.encode(imagePath)
                return RightSideIndicators(rid, path)
            }
        }
    }

    data class ReportViewer(private val reportId: String) : Destinations(
        route = "report_viewer?id=$reportId",
        titleRes = R.string.report_viewer_title,
        labelRes = R.string.report_viewer_title,
        icon = Icons.Rounded.Description
    ) {
        companion object {
            const val routePattern = "report_viewer?id={id}"
            fun create(id: String): ReportViewer = ReportViewer(id)
        }
    }
}

val BottomDestinations = listOf(
    Destinations.Home,
    Destinations.AccountSettings,
    Destinations.Reports,
    Destinations.Feedback
)



