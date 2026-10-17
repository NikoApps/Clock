package org.nikoapps.commons.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = NikoColors.Light.Accent,
    onPrimary = NikoColors.Light.OnAccent,
    secondary = NikoColors.Light.AccentText,
    onSecondary = NikoColors.Paper,
    background = NikoColors.Light.Background,
    onBackground = NikoColors.Light.OnBackground,
    surface = NikoColors.Light.Surface,
    onSurface = NikoColors.Light.OnBackground,
    onSurfaceVariant = NikoColors.Light.OnBackgroundMuted,
    outline = NikoColors.Light.Outline,
    error = NikoColors.Light.Danger,
    onError = NikoColors.Paper,
)

private val DarkScheme = darkColorScheme(
    primary = NikoColors.Dark.Accent,
    onPrimary = NikoColors.Dark.OnAccent,
    secondary = NikoColors.Dark.AccentText,
    onSecondary = NikoColors.Ink,
    background = NikoColors.Dark.Background,
    onBackground = NikoColors.Dark.OnBackground,
    surface = NikoColors.Dark.Surface,
    onSurface = NikoColors.Dark.OnBackground,
    onSurfaceVariant = NikoColors.Dark.OnBackgroundMuted,
    outline = NikoColors.Dark.Outline,
    error = NikoColors.Dark.Danger,
    onError = NikoColors.Ink,
)

/**
 * The shared Niko theme. No dynamic colour: wallpaper-based palettes can break
 * contrast and make apps look different on every phone.
 */
@Composable
fun NikoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = NikoTypography,
        content = content,
    )
}
