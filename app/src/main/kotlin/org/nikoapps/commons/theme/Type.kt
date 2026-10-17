package org.nikoapps.commons.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System font on purpose: Android falls back to Noto for Devanagari, Arabic,
// Bengali and most other scripts, so every language renders without bundling fonts.
// Sizes start larger than Material defaults; the user's font scale still applies on top.
private val Base = TextStyle(fontFamily = FontFamily.Default)

val NikoTypography = Typography(
    displayLarge = Base.copy(fontSize = 88.sp, lineHeight = 96.sp, fontWeight = FontWeight.Bold),
    displayMedium = Base.copy(fontSize = 64.sp, lineHeight = 72.sp, fontWeight = FontWeight.Bold),
    headlineLarge = Base.copy(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = Base.copy(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.copy(fontSize = 22.sp, lineHeight = 32.sp),
    bodyMedium = Base.copy(fontSize = 20.sp, lineHeight = 28.sp),
    labelLarge = Base.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
)
