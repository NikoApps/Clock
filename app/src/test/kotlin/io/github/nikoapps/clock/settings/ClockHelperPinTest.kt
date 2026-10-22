package io.github.nikoapps.clock.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockHelperPinTest {
    /** PINs saved before the move to Commons must still match, so the hash must never change. */
    @Test
    fun hashMatchesPinsAlreadySaved() {
        // printf 'niko-clock-helper-pin:1234' | sha256sum
        assertEquals("196cc4f6a7556ae9b689b0e044a9e7e521470c216e291072775bf4dd58d1ce33", ClockHelperPin.hash("1234"))
    }
}
