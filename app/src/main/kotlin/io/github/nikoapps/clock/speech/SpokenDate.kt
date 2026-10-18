package io.github.nikoapps.clock.speech

import io.github.nikoapps.clock.calendar.BikramSambat
import io.github.nikoapps.clock.settings.Settings
import java.time.LocalDate

/**
 * Everything said when someone asks for the date: the date, the Bikram Sambat date if the
 * helper turned it on and the language uses it, then the helper's note.
 */
fun spokenDate(phraser: TimePhraser, settings: Settings, date: LocalDate): String = listOfNotNull(
    phraser.datePhrase(date),
    if (settings.bikramSambat) BikramSambat.fromGregorian(date)?.let(phraser::bikramSambatPhrase) else null,
    settings.helperNote?.trim()?.takeIf { it.isNotEmpty() },
).joinToString(" ")

/** The morning greeting: good morning, then the date as above. */
fun morningGreeting(phraser: TimePhraser, settings: Settings, date: LocalDate): String =
    "${phraser.goodMorning} ${spokenDate(phraser, settings, date)}"
