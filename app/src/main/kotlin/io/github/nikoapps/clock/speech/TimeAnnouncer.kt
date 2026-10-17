package io.github.nikoapps.clock.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import io.github.nikoapps.clock.settings.SettingsRepository
import io.github.nikoapps.commons.speech.Speaker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Why the time is being spoken. Decides the audio stream and volume. */
enum class Announcement {
    /** A scheduled chime: alarm stream, at the app's own chime volume. */
    Chime,

    /** Someone asked (tile, widget): accessibility stream, which Do Not Disturb never mutes. */
    Request,
}

/**
 * Speaks the current time once and waits until it has been said.
 * Create one per owner and call [shutdown] when the owner goes away.
 */
class TimeAnnouncer(context: Context) {
    private val appContext = context.applicationContext
    private val settings = SettingsRepository(appContext)
    private val log = SpeechLog(appContext)
    private val audio = appContext.getSystemService<AudioManager>()

    private val ready = CompletableDeferred<Boolean>()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val speaker = Speaker(
        context = appContext,
        onReady = { ready.complete(it) },
        onDone = { id -> pending.remove(id)?.complete(Unit) },
    )

    /** Returns true if the time was spoken to the end. */
    suspend fun announce(kind: Announcement): Boolean {
        val current = settings.current()
        val phraser = phraserFor(current) ?: return false
        // Slow phones can take a few seconds to start the engine.
        if (withTimeoutOrNull(ENGINE_START_TIMEOUT_MS) { ready.await() } != true) return false
        // Never fall back to another language: saying nothing is better than the wrong words.
        if (!speaker.useVoiceFor(phraser.locale)) return false

        speaker.setRate(current.speechRate)
        speaker.setPitch(current.voicePitch.pitch)
        val usage = when (kind) {
            Announcement.Chime -> AudioAttributes.USAGE_ALARM
            Announcement.Request -> AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
        }
        speaker.setAudioUsage(usage)
        // Volume is relative to the alarm stream's own volume, so the system volume is never changed.
        val volume = when (kind) {
            Announcement.Chime -> current.chimeVolumePercent.coerceIn(0, 100) / 100f
            Announcement.Request -> 1f
        }

        if (current.vibrateBeforeSpeaking) vibrate()

        val focus = requestFocus(usage)
        try {
            val id = UUID.randomUUID().toString()
            val done = CompletableDeferred<Unit>()
            pending[id] = done
            if (!speaker.speak(phraser.phrase(LocalTime.now()), volume = volume, utteranceId = id)) {
                pending.remove(id)
                return false
            }
            val finished = withTimeoutOrNull(SPEAK_TIMEOUT_MS) { done.await() } != null
            if (finished) log.record(Instant.now(), chime = kind == Announcement.Chime)
            return finished
        } finally {
            focus?.let { audio?.abandonAudioFocusRequest(it) }
        }
    }

    private suspend fun vibrate() {
        val vibrator = appContext.getSystemService<Vibrator>() ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        // Let the buzz finish so it doesn't run over the first word.
        delay(VIBRATION_MS + 200)
    }

    /** Ducks music while speaking. Speech goes ahead even if focus is refused. */
    private fun requestFocus(usage: Int): AudioFocusRequest? {
        val audio = audio ?: return null
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .build()
        return request.takeIf { audio.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED }
    }

    fun shutdown() {
        speaker.shutdown()
        pending.values.forEach { it.cancel() }
        pending.clear()
    }

    private companion object {
        const val ENGINE_START_TIMEOUT_MS = 10_000L
        const val SPEAK_TIMEOUT_MS = 30_000L
        const val VIBRATION_MS = 300L
    }
}
