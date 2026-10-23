package io.github.nikoapps.clock.speech

import io.github.nikoapps.commons.speech.Speaker
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * [Speaker.useVoiceFor], asked again for a few seconds while it finds nothing. When Google's speech
 * engine starts cold, such as just after a restart, it lists its voices as not installed for about a
 * second while it loads them, and the first chime or shake would stay quiet.
 */
suspend fun Speaker.awaitVoiceFor(locale: Locale): Boolean {
    repeat(VOICE_TRIES - 1) {
        if (useVoiceFor(locale)) return true
        delay(VOICE_RETRY_MS)
    }
    return useVoiceFor(locale)
}

private const val VOICE_TRIES = 20
private const val VOICE_RETRY_MS = 500L
