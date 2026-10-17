package io.github.nikoapps.clock.settings

import java.time.LocalTime

enum class ShakeSensitivity { Low, Medium, High }

/** Every setting is a choice of three plain options, never a number to type. */
enum class SpeechSpeed(val rate: Float) { Slow(0.75f), Normal(0.9f), Fast(1.1f) }

enum class TextSize(val scale: Float) { Small(1.0f), Medium(1.2f), Large(1.4f) }

/** Everything a helper can set. Defaults follow the Clock spec. */
data class Settings(
    /** Voice language as a BCP 47 tag such as "ne-NP", or null to follow the phone's language. Separate from the screen language. */
    val language: String? = null,
    /** 1.0 is the engine's normal speed. */
    val speechRate: Float = 0.9f,
    val chimeEnabled: Boolean = false,
    /** 60, 30 or 15. */
    val chimeIntervalMinutes: Int = 60,
    /** Percent of the alarm stream's maximum volume. */
    val chimeVolumePercent: Int = 80,
    val quietHoursEnabled: Boolean = true,
    val quietStart: LocalTime = LocalTime.of(22, 0),
    val quietEnd: LocalTime = LocalTime.of(7, 0),
    /** Chimes stay silent while Do Not Disturb is on. User-triggered speech still answers. */
    val chimeFollowsDoNotDisturb: Boolean = true,
    val shakeEnabled: Boolean = false,
    val shakeSensitivity: ShakeSensitivity = ShakeSensitivity.Medium,
    val vibrateBeforeSpeaking: Boolean = false,
    /** SHA-256 of the helper's 4-digit PIN, or null when there is no PIN. */
    val helperPinHash: String? = null,
    /** On top of the phone's own font size. */
    val textSize: TextSize = TextSize.Medium,
) {
    val speechSpeed: SpeechSpeed
        get() = SpeechSpeed.entries.minBy { kotlin.math.abs(it.rate - speechRate) }

    /** True when [time] falls inside quiet hours, including ranges that cross midnight. */
    fun isQuiet(time: LocalTime): Boolean {
        if (!quietHoursEnabled || quietStart == quietEnd) return false
        return if (quietStart < quietEnd) {
            time >= quietStart && time < quietEnd
        } else {
            time >= quietStart || time < quietEnd
        }
    }
}
