package io.github.nikoapps.clock.speech

import io.github.nikoapps.commons.speech.TimePhraser
import io.github.nikoapps.commons.status.PhoneStatus
import io.github.nikoapps.commons.status.statusPhrasesFor
import java.time.LocalDateTime

/**
 * The phone status: what may stop calls first, then battery, signal and internet, then the time
 * and the date (Hub ADR 0039). Without status sentences for the language, just the time and date.
 */
fun spokenStatus(phraser: TimePhraser, status: PhoneStatus, now: LocalDateTime): String = listOfNotNull(
    statusPhrasesFor(phraser.locale)?.sentence(status)?.takeIf { it.isNotBlank() },
    phraser.phrase(now.toLocalTime()),
    phraser.datePhrase(now.toLocalDate()),
).joinToString(" ")
