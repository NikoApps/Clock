package io.github.nikoapps.clock.shake

import io.github.nikoapps.clock.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class ShakeStepsTest {
    @Test fun `time, date then status by default`() =
        assertEquals(listOf(ShakeStep.Time, ShakeStep.Date, ShakeStep.Status), shakeSteps(Settings()))

    @Test fun `status straight after the time when the date is off`() =
        assertEquals(listOf(ShakeStep.Time, ShakeStep.Status), shakeSteps(Settings(shakeAgainForDate = false)))

    @Test fun `only the time with both off`() =
        assertEquals(listOf(ShakeStep.Time), shakeSteps(Settings(shakeAgainForDate = false, shakeAgainForStatus = false)))
}
