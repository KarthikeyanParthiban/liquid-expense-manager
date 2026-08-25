package com.expensemanager.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Pure Apple / Material Accents
val AppleBlue = Color(0xFF007AFF)
val AppleBlueDark = Color(0xFF0056B3)
val AppleBlueLightConstant = Color(0xFFEBF5FF)

val AppleGreen = Color(0xFF30D158)       // Apple Emerald
val AppleGreenDark = Color(0xFF248A3D)
val AppleGreenLightConstant = Color(0xFFE8F9EE)

val AppleRed = Color(0xFFFF453A)         // Apple Ruby / Coral
val AppleRedDark = Color(0xFFD70015)
val AppleRedLightConstant = Color(0xFFFFEEEE)

val AppleOrange = Color(0xFFFF9F0A)      // Apple Amber / Flame
val ApplePurple = Color(0xFFAF52DE)
val AppleIndigo = Color(0xFF5856D6)
val AppleTeal = Color(0xFF5AC8FA)

// Pure Obsidian OLED Black Dark Palette with Titanium Accents
val DarkBg = Color(0xFF000000)
val DarkBgSubtle = Color(0xFF060606)
val DarkCardSurface = Color(0xFF121212)
val DarkCardSurfaceTranslucent = Color(0xF2121212)
val DarkElevatedSurface = Color(0xFF1C1C1C)

val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFA3A3A3)
val DarkTextTertiary = Color(0xFF737373)

val DarkBorderLight = Color(0xFF2A2A2A)      // Hairline Titanium Wireframe
val DarkBorderSubtle = Color(0xFF202020)
val DarkBorderHighlight = Color(0x38FFFFFF)

// Crisp Light Palette
val LightBgConstant = Color(0xFFF2F4F7)
val LightBgSubtleConstant = Color(0xFFF8FAFC)
val LightCardSurfaceConstant = Color(0xFFFFFFFF)
val LightCardSurfaceTranslucentConstant = Color(0xF2FFFFFF)
val LightElevatedSurfaceConstant = Color(0xFFF3F4F6)

val TextPrimaryConstant = Color(0xFF111827)
val TextSecondaryConstant = Color(0xFF6B7280)
val TextTertiaryConstant = Color(0xFF9CA3AF)

val BorderLightConstant = Color(0xFFE5E7EB)
val BorderSubtleConstant = Color(0xFFF3F4F6)
val BorderHighlightConstant = Color(0x20000000)

// Dynamic Composable Theme Properties (Auto-switch based on LocalIsDarkTheme)
val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkTextPrimary else TextPrimaryConstant

val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkTextSecondary else TextSecondaryConstant

val TextTertiary: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkTextTertiary else TextTertiaryConstant

val LightBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBg else LightBgConstant

val LightBgSubtle: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBgSubtle else LightBgSubtleConstant

val LightCardSurface: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkCardSurface else LightCardSurfaceConstant

val LightCardSurfaceTranslucent: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkCardSurfaceTranslucent else LightCardSurfaceTranslucentConstant

val LightElevatedSurface: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkElevatedSurface else LightElevatedSurfaceConstant

val BorderLight: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBorderLight else BorderLightConstant

val BorderSubtle: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBorderSubtle else BorderSubtleConstant

val BorderHighlight: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBorderHighlight else BorderHighlightConstant

val AppleBlueLight: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1A1A1A) else AppleBlueLightConstant

val AppleGreenLight: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF132819) else AppleGreenLightConstant

val AppleRedLight: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2B1414) else AppleRedLightConstant
