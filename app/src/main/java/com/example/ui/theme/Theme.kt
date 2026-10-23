package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.HiveSettingsPersistence
import com.example.ui.HiveThemeMode
import com.example.ui.HiveThemePreset

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val config = remember(context, HiveSettingsPersistence.changeCounter) {
        HiveSettingsPersistence.loadConfig(context)
    }

    val effectiveDark = when (config.themeMode) {
        HiveThemeMode.SYSTEM -> darkTheme
        HiveThemeMode.DARK -> true
        HiveThemeMode.LIGHT -> false
    }

    val preset = config.themePreset

    val colorScheme = if (effectiveDark) {
        darkColorScheme(
            primary = Color(preset.primaryDarkHex),
            onPrimary = if (preset == HiveThemePreset.CLASSIC_INK || preset == HiveThemePreset.OBSIDIAN_HONEY) Ink950 else Color.White,
            secondary = Color(preset.secondaryDarkHex),
            onSecondary = Ink950,
            background = Color(preset.bgDarkHex),
            onBackground = Ink100,
            surface = Color(preset.surfaceDarkHex),
            onSurface = Ink100,
            surfaceVariant = Ink800,
            onSurfaceVariant = Ink400,
            outline = Ink700
        )
    } else {
        lightColorScheme(
            primary = Color(preset.primaryLightHex),
            onPrimary = Color.White,
            secondary = Ink400,
            onSecondary = Color.White,
            background = Color(preset.bgLightHex),
            onBackground = Ink900,
            surface = Color(preset.surfaceLightHex),
            onSurface = Ink900,
            surfaceVariant = Ink100,
            onSurfaceVariant = Ink400,
            outline = Ink200
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
