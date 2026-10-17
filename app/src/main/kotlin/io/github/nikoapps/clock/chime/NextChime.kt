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
