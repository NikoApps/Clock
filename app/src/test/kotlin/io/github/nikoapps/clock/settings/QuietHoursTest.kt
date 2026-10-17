package io.github.nikoapps.clock.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class QuietHoursTest {
    private val overnight = Settings(quietStart = LocalTime.of(22, 0), quietEnd = LocalTime.of(7, 0))

    @Test fun `quiet late at night`() = assertTrue(overnight.isQuiet(LocalTime.of(23, 30)))
    @Test fun `quiet just after midnight`() = assertTrue(overnight.isQuiet(LocalTime.of(0, 0)))
    @Test fun `quiet at the start`() = assertTrue(overnight.isQuiet(LocalTime.of(22, 0)))
    @Test fun `not quiet at the end`() = assertFalse(overnight.isQuiet(LocalTime.of(7, 0)))
    @Test fun `not quiet in the day`() = assertFalse(overnight.isQuiet(LocalTime.of(12, 0)))
    @Test fun `turned off`() = assertFalse(overnight.copy(quietHoursEnabled = false).isQuiet(LocalTime.of(23, 0)))

    @Test fun `range within one day`() {
        val afternoon = Settings(quietStart = LocalTime.of(13, 0), quietEnd = LocalTime.of(15, 0))
        assertTrue(afternoon.isQuiet(LocalTime.of(14, 0)))
        assertFalse(afternoon.isQuiet(LocalTime.of(16, 0)))
    }
}
