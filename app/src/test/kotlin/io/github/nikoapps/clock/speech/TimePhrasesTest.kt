package io.github.nikoapps.clock.speech

import com.ibm.icu.text.MessageFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

// ICU4J has the same spellout rules as android.icu, so tests run on the JVM.
// Mirrors IcuNumberWords, which can't run here because android.icu is a stub in unit tests.
private fun icuWords(locale: Locale) = object : NumberWords {
    override fun words(number: Int): String = MessageFormat("{0,spellout}", locale).format(arrayOf(number))
    override fun ordinal(number: Int): String =
        MessageFormat("{0,spellout,%spellout-ordinal}", locale).format(arrayOf(number))
}

private val OCT_17 = LocalDate.of(2026, 10, 17)

class EnglishTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.US, ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun date() = assertEquals("Today is Saturday, the seventeenth of October.", phraser.datePhrase(OCT_17))

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

    @Test fun date() = assertEquals("आज शनिबार, अक्टोबर सत्र गते।", phraser.datePhrase(OCT_17))

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

class HindiTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.forLanguageTag("hi-IN"), ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun date() = assertEquals("आज शनिवार, सत्रह अक्तूबर है।", phraser.datePhrase(OCT_17))

    @Test fun `on the hour`() = assertEquals("अभी सुबह के सात बजे हैं।", say(7, 0))
    @Test fun `one o'clock is singular`() = assertEquals("अभी दोपहर का एक बजा है।", say(13, 0))
    @Test fun `quarter past`() = assertEquals("अभी दोपहर के सवा तीन बजे हैं।", say(15, 15))
    @Test fun `one thirty`() = assertEquals("अभी दोपहर के डेढ़ बजे हैं।", say(13, 30))
    @Test fun `two thirty`() = assertEquals("अभी रात के ढाई बजे हैं।", say(2, 30))
    @Test fun `half past`() = assertEquals("अभी शाम के साढ़े छह बजे हैं।", say(18, 30))
    @Test fun `quarter to`() = assertEquals("अभी दोपहर के पौने चार बजे हैं।", say(15, 45))
    @Test fun `other minutes`() = assertEquals("अभी सुबह के दस बजकर बीस मिनट हुए हैं।", say(10, 20))
    @Test fun `never contains digits`() = assertNoDigits(phraser)
}

class SpanishTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.forLanguageTag("es-ES"), ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun date() = assertEquals("Hoy es sábado, diecisiete de octubre.", phraser.datePhrase(OCT_17))

    @Test fun `on the hour`() = assertEquals("Son las siete en punto de la mañana.", say(7, 0))
    @Test fun `one o'clock`() = assertEquals("Es la una en punto de la tarde.", say(13, 0))
    @Test fun `quarter past`() = assertEquals("Son las tres y cuarto de la tarde.", say(15, 15))
    @Test fun `half past`() = assertEquals("Son las seis y media de la tarde.", say(18, 30))
    @Test fun `quarter to counts from the next hour`() = assertEquals("Son las cuatro menos cuarto de la tarde.", say(15, 45))
    @Test fun `minutes to`() = assertEquals("Son las cinco menos veinte de la tarde.", say(16, 40))
    @Test fun `minutes past`() = assertEquals("Son las diez y veinte de la mañana.", say(10, 20))
    @Test fun `just before one`() = assertEquals("Es la una menos diez de la tarde.", say(12, 50))
    @Test fun midnight() = assertEquals("Es medianoche.", say(0, 0))
    @Test fun noon() = assertEquals("Es mediodía.", say(12, 0))
    @Test fun `never contains digits`() = assertNoDigits(phraser)
}

class PortugueseTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.forLanguageTag("pt-BR"), ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun date() = assertEquals("Hoje é sábado, dezessete de outubro.", phraser.datePhrase(OCT_17))
    @Test fun `first of the month`() = assertEquals("Hoje é quinta-feira, primeiro de outubro.", phraser.datePhrase(LocalDate.of(2026, 10, 1)))

    @Test fun `on the hour`() = assertEquals("São sete horas da manhã.", say(7, 0))
    @Test fun `one o'clock`() = assertEquals("É uma hora da tarde.", say(13, 0))
    @Test fun `two is feminine`() = assertEquals("São duas e quinze da tarde.", say(14, 15))
    @Test fun `half past`() = assertEquals("São seis e meia da noite.", say(18, 30))
    @Test fun `other minutes`() = assertEquals("São três e quarenta e cinco da tarde.", say(15, 45))
    @Test fun `small hours`() = assertEquals("São três e vinte da madrugada.", say(3, 20))
    @Test fun midnight() = assertEquals("É meia-noite.", say(0, 0))
    @Test fun noon() = assertEquals("É meio-dia.", say(12, 0))
    @Test fun `never contains digits`() = assertNoDigits(phraser)
}

private val NOV_1 = LocalDate.of(2026, 11, 1)

class FrenchTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.FRANCE, ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun date() = assertEquals("Nous sommes le samedi dix-sept octobre.", phraser.datePhrase(OCT_17))
    @Test fun `first of the month`() = assertEquals("Nous sommes le dimanche premier novembre.", phraser.datePhrase(NOV_1))

    @Test fun `on the hour`() = assertEquals("Il est sept heures du matin.", say(7, 0))
    @Test fun `one o'clock is feminine`() = assertEquals("Il est une heure de l'après-midi.", say(13, 0))
    @Test fun `quarter past`() = assertEquals("Il est neuf heures et quart du soir.", say(21, 15))
    @Test fun `half past`() = assertEquals("Il est trois heures et demie de l'après-midi.", say(15, 30))
    @Test fun `quarter to counts from the next hour`() = assertEquals("Il est quatre heures moins le quart de l'après-midi.", say(15, 45))
    @Test fun `round minutes count down`() = assertEquals("Il est midi moins dix.", say(11, 50))
    @Test fun `other minutes`() = assertEquals("Il est trois heures trente-sept de l'après-midi.", say(15, 37))
    @Test fun noon() = assertEquals("Il est midi et demi.", say(12, 30))
    @Test fun midnight() = assertEquals("Il est minuit.", say(0, 0))
    @Test fun `quarter to midnight`() = assertEquals("Il est minuit moins le quart.", say(23, 45))
    @Test fun `never contains digits`() = assertNoDigits(phraser)
}

class IndonesianTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.forLanguageTag("id-ID"), ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun date() = assertEquals("Hari ini hari Sabtu, tanggal tujuh belas Oktober.", phraser.datePhrase(OCT_17))

    @Test fun `on the hour`() = assertEquals("Sekarang pukul tujuh pagi.", say(7, 0))
    @Test fun `quarter past`() = assertEquals("Sekarang pukul tiga lewat seperempat sore.", say(15, 15))
    @Test fun `half counts towards the next hour`() = assertEquals("Sekarang pukul setengah empat sore.", say(15, 30))
    @Test fun `quarter to`() = assertEquals("Sekarang pukul empat kurang seperempat sore.", say(15, 45))
    @Test fun `minutes past`() = assertEquals("Sekarang pukul tiga lewat dua puluh menit sore.", say(15, 20))
    @Test fun `minutes to`() = assertEquals("Sekarang pukul sembilan kurang sepuluh menit malam.", say(20, 50))
    @Test fun `old language code`() = assertEquals(phraser.phrase(LocalTime.NOON), timePhraserFor(Locale("in"), ::icuWords)!!.phrase(LocalTime.NOON))
    @Test fun `never contains digits`() = assertNoDigits(phraser)
}

class BengaliTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.forLanguageTag("bn-BD"), ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun date() = assertEquals("আজ শনিবার, অক্টোবর মাসের সতেরো তারিখ।", phraser.datePhrase(OCT_17))

    @Test fun `on the hour`() = assertEquals("এখন সকাল সাতটা বাজে।", say(7, 0))
    @Test fun `quarter past`() = assertEquals("এখন বিকেল সোয়া তিনটা বাজে।", say(15, 15))
    @Test fun `half past`() = assertEquals("এখন সন্ধ্যা সাড়ে ছয়টা বাজে।", say(18, 30))
    @Test fun `one thirty`() = assertEquals("এখন দুপুর দেড়টা বাজে।", say(13, 30))
    @Test fun `quarter to counts from the next hour`() = assertEquals("এখন বিকেল পৌনে চারটা বাজে।", say(15, 45))
    @Test fun `other minutes`() = assertEquals("এখন বিকেল তিনটা বেজে বিশ মিনিট।", say(15, 20))
    @Test fun `never contains digits or Latin letters`() = assertOwnScript(phraser)
}

class UrduTimePhrasesTest {
    private val phraser = timePhraserFor(Locale.forLanguageTag("ur-PK"), ::icuWords)!!

    private fun say(hour: Int, minute: Int) = phraser.phrase(LocalTime.of(hour, minute))

    @Test fun date() = assertEquals("آج ہفتہ، سترہ اکتوبر ہے۔", phraser.datePhrase(OCT_17))

    @Test fun `one o'clock is singular`() = assertEquals("ابھی دوپہر کا ایک بجا ہے۔", say(13, 0))
    @Test fun `on the hour`() = assertEquals("ابھی صبح کے سات بجے ہیں۔", say(7, 0))
    @Test fun `quarter past`() = assertEquals("ابھی دوپہر کے سوا تین بجے ہیں۔", say(15, 15))
    @Test fun `two thirty`() = assertEquals("ابھی رات کے ڈھائی بجے ہیں۔", say(2, 30))
    @Test fun `quarter to counts from the next hour`() = assertEquals("ابھی شام کے پونے پانچ بجے ہیں۔", say(16, 45))
    @Test fun `other minutes`() = assertEquals("ابھی دوپہر کے تین بج کر بیس منٹ ہوئے ہیں۔", say(15, 20))
    @Test fun `never contains digits or Latin letters`() = assertOwnScript(phraser)
}

class TimePhraserForTest {
    @Test fun `unsupported language has no phraser`() =
        assertNull(timePhraserFor(Locale.forLanguageTag("sw-KE"), ::icuWords))
}

private fun assertNoDigits(phraser: TimePhraser) {
    for (hour in 0..23) for (minute in 0..59) {
        val phrase = phraser.phrase(LocalTime.of(hour, minute))
        assert(phrase.none { it.isDigit() }) { phrase }
    }
}

/** Bengali and Urdu use their own number tables; no English may leak in from ICU. */
private fun assertOwnScript(phraser: TimePhraser) {
    assertNoDigits(phraser)
    for (hour in 0..23) for (minute in 0..59) {
        val phrase = phraser.phrase(LocalTime.of(hour, minute))
        assert(phrase.none { it in 'a'..'z' || it in 'A'..'Z' }) { phrase }
    }
    for (day in 1..31) {
        val phrase = phraser.datePhrase(LocalDate.of(2026, 1, day))
        assert(phrase.none { it.isDigit() || it in 'a'..'z' || it in 'A'..'Z' }) { phrase }
    }
}
