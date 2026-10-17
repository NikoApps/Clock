package org.nikoapps.commons.theme

import androidx.compose.ui.graphics.Color

// Generated from the Niko colour tokens (Hub: assets/tokens/colors.json).
// Every text pairing meets WCAG AAA (7:1). See ContrastTest.
object NikoColors {
    val Sun = Color(0xFFFFC53D)
    val Ink = Color(0xFF1F1A14)
    val Paper = Color(0xFFFFF9EE)
    val Night = Color(0xFF14110D)

    object Light {
        val Background = Paper
        val Surface = Color(0xFFFFFFFF)
        val OnBackground = Ink
        val OnBackgroundMuted = Color(0xFF4A4238)
        val Accent = Sun
        val OnAccent = Ink
        val AccentText = Color(0xFF6E4700)
        val Outline = Color(0xFF4A4238)
        val Success = Color(0xFF0B5C31)
        val Danger = Color(0xFFA3161A)
        val Info = Color(0xFF0B4F8A)
    }

    object Dark {
        val Background = Night
        val Surface = Color(0xFF221D17)
        val OnBackground = Paper
        val OnBackgroundMuted = Color(0xFFE9E1D3)
        val Accent = Sun
        val OnAccent = Ink
        val AccentText = Sun
        val Outline = Color(0xFFE9E1D3)
        val Success = Color(0xFF7BE3A6)
        val Danger = Color(0xFFFF9A93)
        val Info = Color(0xFF9CC8FF)
    }

    // One background colour per app so people who can't read can tell icons apart.
    object Apps {
        val Clock = Color(0xFFFFC53D)
        val Dialer = Color(0xFF7BE3A6)
        val Reminders = Color(0xFFFF9A93)
        val Light = Color(0xFFFFE58A)
        val Status = Color(0xFF9CC8FF)
        val DayClock = Color(0xFFC9B8FF)
        val Flash = Color(0xFFFFB067)
    }
}
