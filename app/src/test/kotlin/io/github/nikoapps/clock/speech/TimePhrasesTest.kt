package io.github.nikoapps.clock.speech

import com.ibm.icu.text.MessageFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

// ICU4J has the same spellout rules as android.icu, so tests run on the JVM.
// Mirrors IcuNumberWords, which can't run here because android.icu is a stub in unit tests.
private fun icuWords(locale: Locale) = NumberWords { n ->
    MessageFormat("{0,spellout}", locale).format(arrayOf(n))
}

class EnglishTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.US, ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun `on the hour`() = assertEquals("It's seven o'clock in the morning.", say(7, 0))
    @Test fun `quarter past`() = assertEquals("It's quarter past three in the afternoon.", say(15, 15))
    @Test fun `half past`() = assertEquals("It's half past six in the evening.", say(18, 30))
    @Test fun `quarter to counts from the next hour`() = assertEquals("It's quarter to four in the afternoon.", say(15, 45))
    @Test fun `quarter to keeps the current period`() = assertEquals("It's quarter to twelve in the morning.", say(11, 45))
    @Test fun `single digit minutes`() = assertEquals("It's nine oh five at night.", say(21, 5))
    @Test fun `other minutes`() = assertEquals("It's two forty-two in the afternoon.", say(14, 42))
    @Test fun noon() = assertEquals("It's twelve noon.", say(12, 0))
    @Test fun midnight() = assertEquals("It's midnight.", say(0, 0))
    @Test fun `just after midnight`() = assertEquals("It's twelve twenty at night.", say(0, 20))
    @Test fun `never contains digits`() {
        for (hour in 0..23) for (minute in 0..59) {
            val phrase = say(hour, minute)
            assert(phrase.none { it.isDigit() }) { phrase }
        }
    }
}

class NepaliTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.forLanguageTag("ne-NP"), ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun `on the hour`() = assertEquals("अहिले बिहान सात बज्यो।", say(7, 0))
    @Test fun `quarter past`() = assertEquals("अहिले दिउँसो सवा तिन बज्यो।", say(15, 15))
    @Test fun `half past`() = assertEquals("अहिले साँझ साढे छ बज्यो।", say(18, 30))
    @Test fun `one thirty`() = assertEquals("अहिले दिउँसो डेढ बज्यो।", say(13, 30))
    @Test fun `two thirty`() = assertEquals("अहिले राति अढाई बज्यो।", say(2, 30))
    @Test fun `quarter to counts from the next hour`() = assertEquals("अहिले दिउँसो पौने चार बज्यो।", say(15, 45))
    @Test fun `other minutes`() = assertEquals("अहिले बिहान दस बजेर बिस मिनेट गयो।", say(10, 20))
    @Test fun midnight() = assertEquals("अहिले राति बाह्र बज्यो।", say(0, 0))
    @Test fun `never contains digits`() {
        for (hour in 0..23) for (minute in 0..59) {
            val phrase = say(hour, minute)
            assert(phrase.none { it.isDigit() }) { phrase }
        }
    }
}

class TimePhraserForTest {
    @Test fun `unsupported language has no phraser`() =
        assertNull(timePhraserFor(Locale.forLanguageTag("hi-IN"), ::icuWords))
}
