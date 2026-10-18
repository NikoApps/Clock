package io.github.nikoapps.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class PartOfDayTest {
    @Test
    fun hoursMapToParts() {
        assertEquals(PartOfDay.Night, PartOfDay.of(0))
        assertEquals(PartOfDay.Night, PartOfDay.of(4))
        assertEquals(PartOfDay.Morning, PartOfDay.of(5))
        assertEquals(PartOfDay.Morning, PartOfDay.of(11))
        assertEquals(PartOfDay.Afternoon, PartOfDay.of(12))
        assertEquals(PartOfDay.Evening, PartOfDay.of(17))
        assertEquals(PartOfDay.Evening, PartOfDay.of(20))
        assertEquals(PartOfDay.Night, PartOfDay.of(21))
    }
}
