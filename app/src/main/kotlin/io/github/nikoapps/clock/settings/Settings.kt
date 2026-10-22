package io.github.nikoapps.clock.settings

import io.github.nikoapps.commons.settings.SpeechSpeed
import io.github.nikoapps.commons.settings.TextSize
import io.github.nikoapps.commons.settings.VoicePitch
import java.time.LocalTime

enum class ShakeSensitivity { Low, Medium, High }

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
    val voicePitch: VoicePitch = VoicePitch.Normal,
    /** A second shake soon after the time speaks the date. */
    val shakeAgainForDate: Boolean = true,
    /** The main screen shows the day, part of day and date in large words, for people who lose track of the day. */
    val dayClock: Boolean = false,
    /** The day clock shows over the lock screen while the phone is charging. */
    val dayClockOnLockScreen: Boolean = false,
    /** Show and say the Bikram Sambat date next to the Gregorian one. */
    val bikramSambat: Boolean = false,
    /** A short line from the helper, such as "Ram visits today". Shown on the clock and said with the date. */
    val helperNote: String? = null,
    /** Says good morning and the date at [greetingTime] each day. */
    val greetingEnabled: Boolean = false,
    /** 6:00, 7:00 or 8:00. */
    val greetingTime: LocalTime = LocalTime.of(7, 0),
    /** The helper finished or skipped first-run setup. */
    val setupDone: Boolean = false,
) {
    val speechSpeed: SpeechSpeed
        get() = SpeechSpeed.closestTo(speechRate)

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
