package io.github.nikoapps.clock.speech

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** Spells a number out as words in one language, for example 45 -> "forty-five". */
fun interface NumberWords {
    fun words(number: Int): String

    /** "seventeenth". Only languages that say dates with ordinals need it. */
    fun ordinal(number: Int): String = words(number)
}

/**
 * Turns a time into the sentence the voice speaks.
 *
 * Each language gets its own hand-written templates, because word order, quarter-hour
 * words and day periods differ too much to build from one pattern. The result is
 * words only, never digits, so the voice can't read them in the wrong language.
 */
interface TimePhraser {
    val locale: Locale
    fun phrase(time: LocalTime): String

    /** Today's date as a sentence, such as "Today is Saturday, the seventeenth of October." */
    fun datePhrase(date: LocalDate): String
}

private fun LocalDate.dayName(locale: Locale): String = dayOfWeek.getDisplayName(TextStyle.FULL, locale)

// Standalone month names where a language has them ("octubre", not "de octubre").
private fun LocalDate.monthName(locale: Locale): String = month.getDisplayName(TextStyle.FULL_STANDALONE, locale)

/** The phraser for [locale]'s language, or null when Niko can't say the time in it yet. */
fun timePhraserFor(locale: Locale, numberWords: (Locale) -> NumberWords): TimePhraser? =
    when (locale.language) {
        "en" -> EnglishTimePhraser(numberWords(Locale.ENGLISH))
        "ne" -> NepaliTimePhraser(numberWords(NEPALI))
        "hi" -> HindiTimePhraser(numberWords(HINDI))
        "es" -> SpanishTimePhraser(numberWords(SPANISH))
        "pt" -> PortugueseTimePhraser(numberWords(PORTUGUESE))
        "fr" -> FrenchTimePhraser(numberWords(FRENCH))
        // Android still calls Indonesian "in"; newer code says "id".
        "in", "id" -> IndonesianTimePhraser(numberWords(INDONESIAN))
        // ICU has no spellout rules for Bengali or Urdu, so they use their own number tables.
        "bn" -> BengaliTimePhraser()
        "ur" -> UrduTimePhraser()
        else -> null
    }

private val NEPALI: Locale = Locale.forLanguageTag("ne-NP")
private val HINDI: Locale = Locale.forLanguageTag("hi-IN")
private val SPANISH: Locale = Locale.forLanguageTag("es")
private val PORTUGUESE: Locale = Locale.forLanguageTag("pt-BR")
private val FRENCH: Locale = Locale.forLanguageTag("fr-FR")
private val INDONESIAN: Locale = Locale.forLanguageTag("id-ID")
private val BENGALI: Locale = Locale.forLanguageTag("bn-BD")
private val URDU: Locale = Locale.forLanguageTag("ur-PK")

/** The hour on a 12-hour clock: 0 and 12 are 12, 13 is 1. */
private fun clockHour(hour: Int): Int = (hour + 11) % 12 + 1

/**
 * "It's quarter to four in the afternoon."
 * Uses the period of the current time, so 11:45 is "quarter to twelve in the morning".
 */
class EnglishTimePhraser(private val numbers: NumberWords) : TimePhraser {
    override val locale: Locale = Locale.ENGLISH

    override fun phrase(time: LocalTime): String {
        val hour = time.hour
        val minute = time.minute
        if (minute == 0 && hour == 0) return "It's midnight."
        if (minute == 0 && hour == 12) return "It's twelve noon."

        val h = numbers.words(clockHour(hour))
        val next = numbers.words(clockHour(hour + 1))
        val clock = when (minute) {
            0 -> "$h o'clock"
            15 -> "quarter past $h"
            30 -> "half past $h"
            45 -> "quarter to $next"
            in 1..9 -> "$h oh ${numbers.words(minute)}"
            else -> "$h ${numbers.words(minute)}"
        }
        return "It's $clock ${period(hour)}."
    }

    override fun datePhrase(date: LocalDate): String =
        "Today is ${date.dayName(locale)}, the ${numbers.ordinal(date.dayOfMonth)} of ${date.monthName(locale)}."

    private fun period(hour: Int): String = when (hour) {
        in 5..11 -> "in the morning"
        in 12..16 -> "in the afternoon"
        in 17..20 -> "in the evening"
        else -> "at night"
    }
}

/**
 * "अहिले दिउँसो पौने चार बज्यो।"
 * सवा, साढे and पौने for the quarter hours, with डेढ for 1:30 and अढाई for 2:30.
 * Other minutes use "<hour> बजेर <minutes> मिनेट गयो".
 */
class NepaliTimePhraser(private val numbers: NumberWords) : TimePhraser {
    override val locale: Locale = NEPALI

    override fun phrase(time: LocalTime): String {
        val hour = clockHour(time.hour)
        val minute = time.minute
        val h = numbers.words(hour)
        val clock = when {
            minute == 0 -> "$h बज्यो"
            minute == 15 -> "सवा $h बज्यो"
            minute == 30 && hour == 1 -> "डेढ बज्यो"
            minute == 30 && hour == 2 -> "अढाई बज्यो"
            minute == 30 -> "साढे $h बज्यो"
            minute == 45 -> "पौने ${numbers.words(clockHour(time.hour + 1))} बज्यो"
            else -> "$h बजेर ${numbers.words(minute)} मिनेट गयो"
        }
        return "अहिले ${period(time.hour)} $clock।"
    }

    override fun datePhrase(date: LocalDate): String =
        "आज ${date.dayName(locale)}, ${date.monthName(locale)} ${numbers.words(date.dayOfMonth)} गते।"

    private fun period(hour: Int): String = when (hour) {
        in 4..11 -> "बिहान"
        in 12..15 -> "दिउँसो"
        in 16..18 -> "साँझ"
        else -> "राति"
    }
}

/**
 * "अभी दोपहर के पौने चार बजे हैं।"
 * सवा, साढ़े and पौने for the quarter hours, with डेढ़ for 1:30 and ढाई for 2:30.
 * One o'clock takes बजा है, every other hour बजे हैं.
 */
class HindiTimePhraser(private val numbers: NumberWords) : TimePhraser {
    override val locale: Locale = HINDI

    override fun phrase(time: LocalTime): String {
        val hour = clockHour(time.hour)
        val minute = time.minute
        val h = numbers.words(hour)
        val next = clockHour(time.hour + 1)
        val (clock, singular) = when {
            minute == 0 -> "$h ${if (hour == 1) "बजा है" else "बजे हैं"}" to (hour == 1)
            minute == 15 -> "सवा $h बजे हैं" to false
            minute == 30 && hour == 1 -> "डेढ़ बजे हैं" to false
            minute == 30 && hour == 2 -> "ढाई बजे हैं" to false
            minute == 30 -> "साढ़े $h बजे हैं" to false
            minute == 45 -> "पौने ${numbers.words(next)} बजे हैं" to false
            else -> "$h बजकर ${numbers.words(minute)} मिनट हुए हैं" to false
        }
        val of = if (singular) "का" else "के"
        return "अभी ${period(time.hour)} $of $clock।"
    }

    override fun datePhrase(date: LocalDate): String =
        "आज ${date.dayName(locale)}, ${numbers.words(date.dayOfMonth)} ${date.monthName(locale)} है।"

    private fun period(hour: Int): String = when (hour) {
        in 4..11 -> "सुबह"
        in 12..15 -> "दोपहर"
        in 16..19 -> "शाम"
        else -> "रात"
    }
}

/**
 * "Son las cuatro menos cuarto de la tarde."
 * "Es la una" but "son las dos"; after half past, minutes count down to the next hour.
 */
class SpanishTimePhraser(private val numbers: NumberWords) : TimePhraser {
    override val locale: Locale = SPANISH

    override fun phrase(time: LocalTime): String {
        val minute = time.minute
        if (minute == 0 && time.hour == 0) return "Es medianoche."
        if (minute == 0 && time.hour == 12) return "Es mediodía."

        // Past half past, say the next hour: 4:40 is "las cinco menos veinte".
        val countDown = minute > 30
        val hour = clockHour(if (countDown) time.hour + 1 else time.hour)
        val opening = if (hour == 1) "Es la una" else "Son las ${numbers.words(hour)}"
        val minutes = when {
            minute == 0 -> "en punto"
            minute == 15 -> "y cuarto"
            minute == 30 -> "y media"
            minute == 45 -> "menos cuarto"
            countDown -> "menos ${numbers.words(60 - minute)}"
            else -> "y ${numbers.words(minute)}"
        }
        return "$opening $minutes ${period(time.hour)}."
    }

    // The first of the month is "uno", not "primero", in most of Latin America and Spain.
    override fun datePhrase(date: LocalDate): String =
        "Hoy es ${date.dayName(locale)}, ${numbers.words(date.dayOfMonth)} de ${date.monthName(locale)}."

    private fun period(hour: Int): String = when (hour) {
        in 5..11 -> "de la mañana"
        in 12..19 -> "de la tarde"
        else -> "de la noche"
    }
}

/**
 * "São três e quarenta e cinco da tarde."
 * Hours are feminine (uma, duas), and one o'clock takes "é" instead of "são".
 */
class PortugueseTimePhraser(private val numbers: NumberWords) : TimePhraser {
    override val locale: Locale = PORTUGUESE

    override fun phrase(time: LocalTime): String {
        val minute = time.minute
        if (minute == 0 && time.hour == 0) return "É meia-noite."
        if (minute == 0 && time.hour == 12) return "É meio-dia."

        val hour = clockHour(time.hour)
        val h = when (hour) {
            1 -> "uma"
            2 -> "duas"
            else -> numbers.words(hour)
        }
        val opening = if (hour == 1) "É $h" else "São $h"
        val clock = when (minute) {
            0 -> if (hour == 1) "$opening hora" else "$opening horas"
            30 -> "$opening e meia"
            else -> "$opening e ${numbers.words(minute)}"
        }
        return "$clock ${period(time.hour)}."
    }

    // Brazilian Portuguese says "primeiro" for the first of the month.
    override fun datePhrase(date: LocalDate): String {
        val day = if (date.dayOfMonth == 1) "primeiro" else numbers.words(date.dayOfMonth)
        return "Hoje é ${date.dayName(locale)}, $day de ${date.monthName(locale)}."
    }

    private fun period(hour: Int): String = when (hour) {
        in 5..11 -> "da manhã"
        in 12..17 -> "da tarde"
        in 18..23 -> "da noite"
        else -> "da madrugada"
    }
}

/**
 * "Il est quatre heures moins le quart de l'après-midi."
 * Midi and minuit replace twelve. From twenty-five to the hour, round minutes count down
 * ("moins vingt"); other minutes are said as they are ("trois heures trente-sept").
 */
class FrenchTimePhraser(private val numbers: NumberWords) : TimePhraser {
    override val locale: Locale = FRENCH

    override fun phrase(time: LocalTime): String {
        val minute = time.minute
        val countDown = minute == 45 || minute in listOf(35, 40, 50, 55)
        val hour = if (countDown) time.hour + 1 else time.hour
        val noonOrMidnight = hour % 12 == 0
        val minutes = when {
            minute == 0 -> ""
            minute == 15 -> " et quart"
            // "Demie" agrees with heure; midi and minuit are masculine.
            minute == 30 -> if (noonOrMidnight) " et demi" else " et demie"
            minute == 45 -> " moins le quart"
            countDown -> " moins ${numbers.words(60 - minute)}"
            else -> " ${numbers.words(minute)}"
        }
        return "Il est ${hourName(hour)}$minutes${period(hour)}."
    }

    override fun datePhrase(date: LocalDate): String {
        val day = if (date.dayOfMonth == 1) "premier" else numbers.words(date.dayOfMonth)
        return "Nous sommes le ${date.dayName(locale)} $day ${date.monthName(locale)}."
    }

    private fun hourName(hour: Int): String = when (hour % 24) {
        0 -> "minuit"
        12 -> "midi"
        else -> clockHour(hour).let { if (it == 1) "une heure" else "${numbers.words(it)} heures" }
    }

    private fun period(hour: Int): String = when (hour % 24) {
        0, 12 -> ""
        in 1..11 -> " du matin"
        in 13..17 -> " de l'après-midi"
        else -> " du soir"
    }
}

/**
 * "Sekarang pukul empat kurang seperempat sore."
 * Setengah counts towards the next hour: 3:30 is "setengah empat".
 * After half past, minutes count down with kurang.
 */
class IndonesianTimePhraser(private val numbers: NumberWords) : TimePhraser {
    override val locale: Locale = INDONESIAN

    override fun phrase(time: LocalTime): String {
        val minute = time.minute
        val h = numbers.words(clockHour(time.hour))
        val next = numbers.words(clockHour(time.hour + 1))
        val clock = when {
            minute == 0 -> h
            minute == 15 -> "$h lewat seperempat"
            minute == 30 -> "setengah $next"
            minute == 45 -> "$next kurang seperempat"
            minute > 30 -> "$next kurang ${numbers.words(60 - minute)} menit"
            else -> "$h lewat ${numbers.words(minute)} menit"
        }
        return "Sekarang pukul $clock ${period(time.hour)}."
    }

    override fun datePhrase(date: LocalDate): String =
        "Hari ini hari ${date.dayName(locale)}, tanggal ${numbers.words(date.dayOfMonth)} ${date.monthName(locale)}."

    private fun period(hour: Int): String = when (hour) {
        in 4..10 -> "pagi"
        in 11..14 -> "siang"
        in 15..17 -> "sore"
        else -> "malam"
    }
}

/**
 * "এখন বিকেল পৌনে চারটা বাজে।"
 * সোয়া, সাড়ে and পৌনে for the quarter hours, with দেড়টা for 1:30 and আড়াইটা for 2:30.
 * Other minutes use "<hour> বেজে <minutes> মিনিট".
 */
class BengaliTimePhraser : TimePhraser {
    override val locale: Locale = BENGALI

    override fun phrase(time: LocalTime): String {
        val hour = clockHour(time.hour)
        val minute = time.minute
        val h = BENGALI_HOURS[hour - 1]
        val clock = when {
            minute == 0 -> "$h বাজে"
            minute == 15 -> "সোয়া $h বাজে"
            minute == 30 && hour == 1 -> "দেড়টা বাজে"
            minute == 30 && hour == 2 -> "আড়াইটা বাজে"
            minute == 30 -> "সাড়ে $h বাজে"
            minute == 45 -> "পৌনে ${BENGALI_HOURS[clockHour(time.hour + 1) - 1]} বাজে"
            else -> "$h বেজে ${BENGALI_NUMBERS[minute - 1]} মিনিট"
        }
        return "এখন ${period(time.hour)} $clock।"
    }

    override fun datePhrase(date: LocalDate): String =
        "আজ ${date.dayName(locale)}, ${date.monthName(locale)} মাসের ${BENGALI_NUMBERS[date.dayOfMonth - 1]} তারিখ।"

    private fun period(hour: Int): String = when (hour) {
        in 4..11 -> "সকাল"
        in 12..14 -> "দুপুর"
        in 15..17 -> "বিকেল"
        in 18..19 -> "সন্ধ্যা"
        else -> "রাত"
    }
}

/**
 * "ابھی شام کے پونے چار بجے ہیں۔"
 * Like Hindi: سوا، ساڑھے and پونے for the quarter hours, with ڈیڑھ for 1:30 and ڈھائی for 2:30.
 * One o'clock takes بجا ہے، every other hour بجے ہیں.
 */
class UrduTimePhraser : TimePhraser {
    override val locale: Locale = URDU

    override fun phrase(time: LocalTime): String {
        val hour = clockHour(time.hour)
        val minute = time.minute
        val h = URDU_NUMBERS[hour - 1]
        val next = URDU_NUMBERS[clockHour(time.hour + 1) - 1]
        val (clock, singular) = when {
            minute == 0 -> "$h ${if (hour == 1) "بجا ہے" else "بجے ہیں"}" to (hour == 1)
            minute == 15 -> "سوا $h بجے ہیں" to false
            minute == 30 && hour == 1 -> "ڈیڑھ بجے ہیں" to false
            minute == 30 && hour == 2 -> "ڈھائی بجے ہیں" to false
            minute == 30 -> "ساڑھے $h بجے ہیں" to false
            minute == 45 -> "پونے $next بجے ہیں" to false
            else -> "$h بج کر ${URDU_NUMBERS[minute - 1]} منٹ ہوئے ہیں" to false
        }
        val of = if (singular) "کا" else "کے"
        return "ابھی ${period(time.hour)} $of $clock۔"
    }

    override fun datePhrase(date: LocalDate): String =
        "آج ${date.dayName(locale)}، ${URDU_NUMBERS[date.dayOfMonth - 1]} ${date.monthName(locale)} ہے۔"

    private fun period(hour: Int): String = when (hour) {
        in 4..11 -> "صبح"
        in 12..15 -> "دوپہر"
        in 16..19 -> "شام"
        else -> "رات"
    }
}

// Hours as they're said on a clock: একটা, দুইটা ... বারোটা.
private val BENGALI_HOURS = listOf(
    "একটা", "দুইটা", "তিনটা", "চারটা", "পাঁচটা", "ছয়টা", "সাতটা", "আটটা", "নয়টা", "দশটা", "এগারোটা", "বারোটা",
)

// One to fifty-nine. Bengali and Urdu numbers below a hundred are irregular, so they're listed.
private val BENGALI_NUMBERS = listOf(
    "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়", "দশ",
    "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোলো", "সতেরো", "আঠারো", "উনিশ", "বিশ",
    "একুশ", "বাইশ", "তেইশ", "চব্বিশ", "পঁচিশ", "ছাব্বিশ", "সাতাশ", "আটাশ", "উনত্রিশ", "ত্রিশ",
    "একত্রিশ", "বত্রিশ", "তেত্রিশ", "চৌত্রিশ", "পঁয়ত্রিশ", "ছত্রিশ", "সাঁইত্রিশ", "আটত্রিশ", "উনচল্লিশ", "চল্লিশ",
    "একচল্লিশ", "বিয়াল্লিশ", "তেতাল্লিশ", "চুয়াল্লিশ", "পঁয়তাল্লিশ", "ছেচল্লিশ", "সাতচল্লিশ", "আটচল্লিশ", "উনপঞ্চাশ", "পঞ্চাশ",
    "একান্ন", "বাহান্ন", "তিপ্পান্ন", "চুয়ান্ন", "পঞ্চান্ন", "ছাপ্পান্ন", "সাতান্ন", "আটান্ন", "উনষাট",
)

private val URDU_NUMBERS = listOf(
    "ایک", "دو", "تین", "چار", "پانچ", "چھ", "سات", "آٹھ", "نو", "دس",
    "گیارہ", "بارہ", "تیرہ", "چودہ", "پندرہ", "سولہ", "سترہ", "اٹھارہ", "انیس", "بیس",
    "اکیس", "بائیس", "تئیس", "چوبیس", "پچیس", "چھبیس", "ستائیس", "اٹھائیس", "انتیس", "تیس",
    "اکتیس", "بتیس", "تینتیس", "چونتیس", "پینتیس", "چھتیس", "سینتیس", "اڑتیس", "انتالیس", "چالیس",
    "اکتالیس", "بیالیس", "تینتالیس", "چوالیس", "پینتالیس", "چھیالیس", "سینتالیس", "اڑتالیس", "انچاس", "پچاس",
    "اکاون", "باون", "ترپن", "چون", "پچپن", "چھپن", "ستاون", "اٹھاون", "انسٹھ",
)
