package io.github.nikoapps.clock.speech

import com.ibm.icu.text.MessageFormat
import io.github.nikoapps.commons.speech.NumberWords
import io.github.nikoapps.commons.speech.timePhraserFor
import io.github.nikoapps.commons.status.BatteryLevel
import io.github.nikoapps.commons.status.Connection
import io.github.nikoapps.commons.status.PhoneStatus
import io.github.nikoapps.commons.status.Signal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

private fun words(locale: Locale) = object : NumberWords {
    override fun words(number: Int): String = MessageFormat("{0,spellout}", locale).format(arrayOf(number))
    override fun ordinal(number: Int): String =
        MessageFormat("{0,spellout,%spellout-ordinal}", locale).format(arrayOf(number))
}

class SpokenStatusTest {
    private val english = timePhraserFor(Locale.US, ::words)!!
    private val evening = LocalDateTime.of(2026, 10, 18, 18, 0)

    @Test fun `warnings first, then battery, signal, internet, time and date`() {
        val status = PhoneStatus(
            airplaneMode = true,
            battery = BatteryLevel.AlmostFull,
            connection = Connection.WiFi,
        )
        val said = spokenStatus(english, status, evening)
        assertTrue(said, said.startsWith("Airplane mode is on. Calls and messages can't come in. The battery is almost full. Connected to Wi-Fi. "))
        assertTrue(said, said.endsWith(english.phrase(evening.toLocalTime()) + " " + english.datePhrase(evening.toLocalDate())))
    }

    @Test fun `nothing known says just the time and date`() = assertEquals(
        english.phrase(evening.toLocalTime()) + " " + english.datePhrase(evening.toLocalDate()),
        spokenStatus(english, PhoneStatus(), evening),
    )

    @Test fun `every voice language says the status`() {
        listOf("ne-NP", "hi-IN", "es", "pt-BR", "fr-FR", "id-ID", "bn-BD", "ur-PK").forEach { tag ->
            val phraser = timePhraserFor(Locale.forLanguageTag(tag), ::words)!!
            val said = spokenStatus(phraser, PhoneStatus(battery = BatteryLevel.Low, signal = Signal.Weak), evening)
            assertTrue("$tag: $said", said.length > phraser.phrase(evening.toLocalTime()).length + phraser.datePhrase(evening.toLocalDate()).length + 2)
        }
    }
}
