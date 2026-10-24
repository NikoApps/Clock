package io.github.nikoapps.clock.speech

import io.github.nikoapps.clock.calendar.BikramSambat
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.commons.speech.IcuNumberWords
import io.github.nikoapps.commons.speech.NumberWords
import io.github.nikoapps.commons.speech.TimePhraser
import java.time.LocalDate
import java.util.Locale

/**
 * Everything said when someone asks for the date: the date, the Bikram Sambat date if the
 * helper turned it on and the language uses it, then the helper's note.
 * [numberWords] is only swapped in tests, where android.icu is a stub.
 */
fun spokenDate(
    phraser: TimePhraser,
    settings: Settings,
    date: LocalDate,
    numberWords: (Locale) -> NumberWords = ::IcuNumberWords,
): String = listOfNotNull(
    phraser.datePhrase(date),
    if (settings.bikramSambat) {
        BikramSambat.fromGregorian(date)?.let { bikramSambatPhrase(phraser.locale, numberWords(phraser.locale), it) }
    } else {
        null
    },
    settings.helperNote?.trim()?.takeIf { it.isNotEmpty() },
).joinToString(" ")

/** The morning greeting: good morning, then the date as above. */
fun morningGreeting(
    phraser: TimePhraser,
    settings: Settings,
    date: LocalDate,
    numberWords: (Locale) -> NumberWords = ::IcuNumberWords,
): String = "${phraser.goodMorning} ${spokenDate(phraser, settings, date, numberWords)}"

