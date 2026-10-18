package io.github.nikoapps.clock.speech

import com.ibm.icu.text.MessageFormat
import io.github.nikoapps.clock.calendar.BsDate
import io.github.nikoapps.clock.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

private fun words(locale: Locale) = object : NumberWords {
    override fun words(number: Int): String = MessageFormat("{0,spellout}", locale).format(arrayOf(number))
    override fun ordinal(number: Int): String =
        MessageFormat("{0,spellout,%spellout-ordinal}", locale).format(arrayOf(number))
}

class SpokenDateTest {
    private val english = timePhraserFor(Locale.US, ::words)!!
    private val nepali = timePhraserFor(Locale.forLanguageTag("ne-NP"), ::words)!!
    private val spanish = timePhraserFor(Locale.forLanguageTag("es"), ::words)!!
    private val oct18 = LocalDate.of(2026, 10, 18)

    @Test fun `english bikram sambat`() =
        assertEquals("In the Nepali calendar, it is the first of Kartik, two thousand eighty-three.", english.bikramSambatPhrase(BsDate(2083, 7, 1)))

    @Test fun `nepali bikram sambat`() =
        assertEquals("विक्रम संवत् दुई हजार त्रियासी, कात्तिक एक गते।", nepali.bikramSambatPhrase(BsDate(2083, 7, 1)))

    @Test fun `other languages leave it out`() = assertNull(spanish.bikramSambatPhrase(BsDate(2083, 7, 1)))

    @Test fun `date alone by default`() =
        assertEquals("Today is Sunday, the eighteenth of October.", spokenDate(english, Settings(), oct18))

    @Test fun `date, nepali date and note`() = assertEquals(
        "Today is Sunday, the eighteenth of October. In the Nepali calendar, it is the first of Kartik, two thousand eighty-three. Ram visits today.",
        spokenDate(english, Settings(bikramSambat = true, helperNote = " Ram visits today. "), oct18),
    )

    @Test fun `spanish skips the nepali date but keeps the note`() = assertEquals(
        "Hoy es domingo, dieciocho de octubre. Médico a las tres.",
        spokenDate(spanish, Settings(bikramSambat = true, helperNote = "Médico a las tres."), oct18),
    )

    @Test fun greeting() = assertEquals(
        "शुभ प्रभात। आज आइतबार, अक्टोबर अठार गते।",
        morningGreeting(nepali, Settings(), oct18),
    )
}
