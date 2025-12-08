package com.example.postureapp.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val AccentBlue = Color(0xFF0A84FF)
val SuccessGreen = Color(0xFF34C759)
val DestructiveRed = Color(0xFFFF3B30)

val LightBackground = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE5E5EA)
val LightOutline = Color(0x33000000)

val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1C1C1E)
val DarkSurfaceVariant = Color(0x662C2C2E)
val DarkOutline = Color(0x33FFFFFF)

val TextPrimary = Color(0xFF000000)
val TextSecondary = Color(0x993C3C43)
val TextPrimaryDark = Color(0xFFF2F2F7)
val TextSecondaryDark = Color(0x99EBEBF5)

val LightColors = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlue.copy(alpha = 0.15f),
    onPrimaryContainer = AccentBlue,
    secondary = Color(0xFF5E5CE6),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = LightOutline,
    error = DestructiveRed,
    onError = Color.White
)

val DarkColors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0F5DBD),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF7D7CFF),
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkOutline,
    error = DestructiveRed,
    onError = Color.White
)

