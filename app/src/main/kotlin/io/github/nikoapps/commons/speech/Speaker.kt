package io.github.nikoapps.commons.speech

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.UUID

/**
 * Small wrapper around the phone's text-to-speech engine.
 *
 * Speaks on the alarm stream by default: talking apps on the media stream are a
 * common complaint because media volume is often turned down.
 */
class Speaker(
    context: Context,
    audioUsage: Int = AudioAttributes.USAGE_ALARM,
    private val onReady: (Boolean) -> Unit = {},
) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)

    var isReady: Boolean = false
        private set

    init {
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(audioUsage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
    }

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        onReady(isReady)
    }

    /** Returns false if the engine has no voice for [locale]. */
    fun setLanguage(locale: Locale): Boolean {
        if (!isReady) return false
        val result = tts.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun isLanguageAvailable(locale: Locale): Boolean =
        isReady && tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE

    /** Speaks [text], replacing anything already being spoken unless [queue] is true. */
    fun speak(text: String, queue: Boolean = false) {
        if (!isReady) return
        val mode = if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        tts.speak(text, mode, null, UUID.randomUUID().toString())
    }

    fun stop() {
        tts.stop()
    }

    /** Call when the owner is destroyed. */
    fun shutdown() {
        tts.stop()
        tts.shutdown()
        isReady = false
    }
}
