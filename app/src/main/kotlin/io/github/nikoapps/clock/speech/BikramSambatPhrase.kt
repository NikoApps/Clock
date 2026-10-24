package io.github.nikoapps.clock.speech

import io.github.nikoapps.clock.calendar.BsDate
import io.github.nikoapps.clock.calendar.LATIN_MONTHS
import io.github.nikoapps.clock.calendar.NEPALI_MONTHS
import io.github.nikoapps.commons.speech.NumberWords
import java.util.Locale

/**
 * The Bikram Sambat date as a sentence in [locale]'s language, or null in languages whose
 * speakers don't use it. Lives here rather than in Commons because only Clock says it.
 */
fun bikramSambatPhrase(locale: Locale, numbers: NumberWords, date: BsDate): String? = when (locale.language) {
    "en" -> "In the Nepali calendar, it is the ${numbers.ordinal(date.day)} of ${LATIN_MONTHS[date.month - 1]}, ${numbers.words(date.year)}."
    "ne" -> "विक्रम संवत् ${numbers.words(date.year)}, ${NEPALI_MONTHS[date.month - 1]} ${numbers.words(date.day)} गते।"
    else -> null
}
