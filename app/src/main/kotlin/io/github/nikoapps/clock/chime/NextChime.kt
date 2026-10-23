package io.github.nikoapps.clock.chime

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Chime intervals a helper can pick. Each divides an hour evenly. */
val CHIME_INTERVALS = listOf(60, 30, 15)

/**
 * The next chime strictly after [now], on the minute, where the local minute is a multiple
 * of [intervalMinutes]. Unknown intervals fall back to hourly.
 *
 * Walks forward a minute at a time on the real timeline, so daylight saving changes come out
 * the way the wall clock shows them: a skipped hour gets no chime, a repeated hour chimes again.
 */
fun nextChimeAfter(now: ZonedDateTime, intervalMinutes: Int): ZonedDateTime {
    val interval = intervalMinutes.takeIf { it in CHIME_INTERVALS } ?: 60
    var next = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
    while (next.minute % interval != 0) next = next.plusMinutes(1)
    return next
}

/**
 * How far back to look when putting the chime back after the clock changes, the phone restarts or
 * the app updates. Android hands out the "time changed" news one app at a time, which took 12
 * seconds on a Samsung A01, so a chime can come due before Clock hears of the change.
 */
const val RESTORE_LOOKBACK_SECONDS = 60L

/** The chime to schedule after a clock change: one that came due a moment ago fires straight away. */
fun chimeToRestore(now: ZonedDateTime, intervalMinutes: Int): ZonedDateTime =
    nextChimeAfter(now.minusSeconds(RESTORE_LOOKBACK_SECONDS), intervalMinutes)
