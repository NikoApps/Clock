package io.github.nikoapps.clock.speech

import java.time.LocalTime
import java.util.Locale

/** Spells a number out as words in one language, for example 45 -> "forty-five". */
fun interface NumberWords {
    fun words(number: Int): String
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
}

/** The phraser for [locale]'s language, or null when Niko can't say the time in it yet. */
fun timePhraserFor(locale: Locale, numberWords: (Locale) -> NumberWords): TimePhraser? =
    when (locale.language) {
        "en" -> EnglishTimePhraser(numberWords(Locale.ENGLISH))
        "ne" -> NepaliTimePhraser(numberWords(NEPALI))
        else -> null
    }

private val NEPALI: Locale = Locale.forLanguageTag("ne-NP")

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

    private fun period(hour: Int): String = when (hour) {
        in 4..11 -> "बिहान"
        in 12..15 -> "दिउँसो"
        in 16..18 -> "साँझ"
        else -> "राति"
    }
}
