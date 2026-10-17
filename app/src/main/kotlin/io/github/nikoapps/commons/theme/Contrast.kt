package io.github.nikoapps.commons.theme

import kotlin.math.pow

/** WCAG 2 contrast ratio between two opaque ARGB colours. */
fun contrastRatio(argbA: Long, argbB: Long): Double {
    val la = relativeLuminance(argbA)
    val lb = relativeLuminance(argbB)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

private fun relativeLuminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val c = ((argb shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}
