package io.github.nikoapps.clock.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BikramSambatTest {
    @Test fun `first day of the table`() = assertEquals(BsDate(2000, 1, 1), BikramSambat.fromGregorian(LocalDate.of(1943, 4, 14)))
    @Test fun `day before the table`() = assertNull(BikramSambat.fromGregorian(LocalDate.of(1943, 4, 13)))
    @Test fun `after the table`() = assertNull(BikramSambat.fromGregorian(LocalDate.of(2034, 4, 14)))
    @Test fun `last day of the table`() = assertEquals(BsDate(2090, 12, 30), BikramSambat.fromGregorian(LocalDate.of(2034, 4, 13)))
    @Test fun today() = assertEquals(BsDate(2083, 7, 1), BikramSambat.fromGregorian(LocalDate.of(2026, 10, 18)))

    @Test fun `display in Nepali`() = assertEquals("१ कात्तिक २०८३", BsDate(2083, 7, 1).display(nepali = true))
    @Test fun `display in English`() = assertEquals("1 Kartik 2083", BsDate(2083, 7, 1).display(nepali = false))

    // Gregorian year, month, day, then the expected Bikram Sambat date, from medic/bikram-sambat's test data.
    @Test fun `matches published conversions`() {
        val cases = listOf(
            listOf(1990, 11, 20, 2047, 8, 4),
            listOf(2027, 12, 22, 2084, 9, 7),
            listOf(2000, 7, 10, 2057, 3, 26),
            listOf(2027, 3, 6, 2083, 11, 22),
            listOf(2033, 3, 21, 2089, 12, 7),
            listOf(2020, 5, 22, 2077, 2, 9),
            listOf(1992, 1, 29, 2048, 10, 15),
            listOf(1990, 11, 2, 2047, 7, 16),
            listOf(1995, 11, 11, 2052, 7, 25),
            listOf(2027, 1, 6, 2083, 9, 22),
            listOf(2003, 1, 1, 2059, 9, 17),
            listOf(1967, 5, 2, 2024, 1, 19),
            listOf(1966, 11, 11, 2023, 7, 26),
            listOf(2022, 6, 5, 2079, 2, 22),
            listOf(1996, 3, 24, 2052, 12, 11),
            listOf(1992, 12, 25, 2049, 9, 10),
            listOf(2006, 10, 24, 2063, 7, 7),
            listOf(2005, 5, 21, 2062, 2, 8),
            listOf(2021, 6, 12, 2078, 2, 29),
            listOf(1966, 12, 31, 2023, 9, 16),
            listOf(1958, 9, 23, 2015, 6, 7),
            listOf(1990, 5, 14, 2047, 1, 31),
            listOf(1977, 7, 7, 2034, 3, 23),
            listOf(1963, 1, 2, 2019, 9, 18),
            listOf(1958, 6, 2, 2015, 2, 20),
            listOf(1998, 8, 19, 2055, 5, 3),
            listOf(2022, 12, 10, 2079, 8, 24),
            listOf(2030, 3, 8, 2086, 11, 24),
            listOf(2012, 7, 7, 2069, 3, 23),
            listOf(2007, 3, 23, 2063, 12, 9),
            listOf(1954, 1, 14, 2010, 10, 1),
            listOf(2003, 9, 22, 2060, 6, 5),
            listOf(1985, 11, 3, 2042, 7, 18),
            listOf(1990, 12, 10, 2047, 8, 24),
            listOf(2008, 12, 20, 2065, 9, 5),
            listOf(2016, 8, 5, 2073, 4, 21),
            listOf(2005, 7, 12, 2062, 3, 28),
            listOf(2008, 8, 16, 2065, 4, 32),
            listOf(1964, 6, 3, 2021, 2, 21),
            listOf(2006, 3, 17, 2062, 12, 4),
        )
        for ((gy, gm, gd, by, bm, bd) in cases.map { it.toIntArray() }) {
            assertEquals("$gy-$gm-$gd", BsDate(by, bm, bd), BikramSambat.fromGregorian(LocalDate.of(gy, gm, gd)))
        }
    }
}

private operator fun IntArray.component6() = this[5]
