package io.github.nikoapps.clock.chime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextGreetingTest {
    private val zone = ZoneId.of("Asia/Kathmandu")
    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0) = ZonedDateTime.of(2026, 10, day, hour, minute, second, 0, zone)
    private val seven = LocalTime.of(7, 0)

    @Test fun `later today`() = assertEquals(at(18, 7, 0), nextGreetingAfter(at(18, 5, 30), seven))
    @Test fun `already passed goes to tomorrow`() = assertEquals(at(19, 7, 0), nextGreetingAfter(at(18, 9, 0), seven))
    @Test fun `exactly on time goes to tomorrow`() = assertEquals(at(19, 7, 0), nextGreetingAfter(at(18, 7, 0), seven))
    @Test fun `a moment early stays today`() = assertEquals(at(18, 7, 0), nextGreetingAfter(at(18, 6, 59, 59), seven))
}
