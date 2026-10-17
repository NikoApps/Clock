package io.github.nikoapps.clock.chime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextChimeTest {
    private val kathmandu = ZoneId.of("Asia/Kathmandu")
    private val newYork = ZoneId.of("America/New_York")

    private fun at(hour: Int, minute: Int, second: Int = 0) =
        ZonedDateTime.of(LocalDateTime.of(2026, 10, 17, hour, minute, second), kathmandu)

    @Test fun `hourly goes to the next hour`() = assertEquals(at(15, 0), nextChimeAfter(at(14, 20), 60))
    @Test fun `half hourly goes to half past`() = assertEquals(at(14, 30), nextChimeAfter(at(14, 20), 30))
    @Test fun `quarter hourly goes to the next quarter`() = assertEquals(at(14, 30), nextChimeAfter(at(14, 16), 15))
    @Test fun `on a chime the next one is the following chime`() = assertEquals(at(15, 0), nextChimeAfter(at(14, 0), 60))
    @Test fun `seconds into the minute still land on the minute`() = assertEquals(at(14, 15), nextChimeAfter(at(14, 0, 59), 15))
    @Test fun `a moment before the hour goes to that hour`() = assertEquals(at(15, 0), nextChimeAfter(at(14, 59, 59), 60))
    @Test fun `crosses midnight`() = assertEquals(at(0, 0).plusDays(1), nextChimeAfter(at(23, 45), 60))
    @Test fun `unknown interval falls back to hourly`() = assertEquals(at(15, 0), nextChimeAfter(at(14, 20), 7))

    @Test fun `spring forward skips the missing hour`() {
        // 2026-03-08 02:00 doesn't exist in New York: clocks go from 01:59 to 03:00.
        val before = ZonedDateTime.of(LocalDateTime.of(2026, 3, 8, 1, 30), newYork)
        val next = nextChimeAfter(before, 60)
        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 0), next.toLocalDateTime())
        assertEquals(30L, Duration.between(before, next).toMinutes())
    }

    @Test fun `fall back chimes again in the repeated hour`() {
        // 2026-11-01 01:00 to 01:59 happens twice in New York.
        val first = ZonedDateTime.of(LocalDateTime.of(2026, 11, 1, 1, 0), newYork).withEarlierOffsetAtOverlap()
        val next = nextChimeAfter(first.plusMinutes(30), 60)
        assertEquals(LocalDateTime.of(2026, 11, 1, 1, 0), next.toLocalDateTime())
        assertEquals(60L, Duration.between(first, next).toMinutes())
    }
}
