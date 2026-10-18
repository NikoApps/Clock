package io.github.nikoapps.commons.speech

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID

/**
 * Small wrapper around the phone's text-to-speech engine.
 *
 * Speaks on the alarm stream by default: talking apps on the media stream are a
 * common complaint because media volume is often turned down.
 *
 * [onDone] is called with the utterance ID once it has finished, failed or been cut off,
 * on a background thread.
 */
class Speaker(
    context: Context,
    audioUsage: Int = AudioAttributes.USAGE_ALARM,
    private val onReady: (Boolean) -> Unit = {},
    private val onDone: (String) -> Unit = {},
) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)

    var isReady: Boolean = false
        private set

    init {
        // Slightly slower than the engine default, so the time is easy to follow on the first hearing.
        tts.setSpeechRate(0.9f)
        setAudioUsage(audioUsage)
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) = Unit
                override fun onDone(utteranceId: String) = onDone.invoke(utteranceId)

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) = onDone.invoke(utteranceId)
                override fun onError(utteranceId: String, errorCode: Int) = onDone.invoke(utteranceId)
                override fun onStop(utteranceId: String, interrupted: Boolean) = onDone.invoke(utteranceId)
            },
        )
    }

    /** An [AudioAttributes] usage, such as [AudioAttributes.USAGE_ALARM]. Applies to the next [speak]. */
    fun setAudioUsage(usage: Int) {
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
    }

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        onReady(isReady)
    }

    /** 1.0 is the engine's normal speed. */
    fun setRate(rate: Float) {
        tts.setSpeechRate(rate)
    }

    /** 1.0 is the voice's normal pitch. */
    fun setPitch(pitch: Float) {
        tts.setPitch(pitch)
    }

    /** Returns false if the engine has no voice for [locale]. */
    fun setLanguage(locale: Locale): Boolean {
        if (!isReady) return false
        val result = tts.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    /**
     * Picks an installed, offline voice for [locale]'s language, preferring the same country
     * and the best quality. Returns false if there is none: a listed voice can still be
     * downloading or need the internet, and Android would then fail or quietly use another voice.
     */
    fun useVoiceFor(locale: Locale): Boolean {
        if (!isReady) return false
        // Some engines return null or throw here instead of an empty set.
        val voices = runCatching { tts.voices }.getOrNull().orEmpty()
        if (voices.isEmpty()) {
            // Engines that don't list voices (older eSpeak builds) only support setLanguage.
            return setLanguage(locale)
        }
        val best = voices
            .filter { it.locale.language == locale.language && it.isUsableOffline() }
            .sortedWith(
                compareByDescending<Voice> { it.locale.country == locale.country }
                    .thenByDescending { it.quality },
            )
            .firstOrNull()
            ?: return false
        return tts.setVoice(best) == TextToSpeech.SUCCESS
    }

    private fun Voice.isUsableOffline(): Boolean =
        !isNetworkConnectionRequired &&
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features.orEmpty()

    /** The speech engine's name as the phone shows it, such as "Speech Services by Google". */
    fun engineName(): String? {
        if (!isReady) return null
        val default = tts.defaultEngine ?: return null
        return runCatching { tts.engines.firstOrNull { it.name == default }?.label }.getOrNull()
    }

    fun isLanguageAvailable(locale: Locale): Boolean =
        isReady && tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE

    /**
     * Speaks [text], replacing anything already being spoken unless [queue] is true.
     * [volume] runs from 0 to 1, relative to the stream's current volume.
     * Returns false if the engine didn't accept it, in which case [onDone] isn't called.
     */
    fun speak(
        text: String,
        queue: Boolean = false,
        volume: Float = 1f,
        utteranceId: String = UUID.randomUUID().toString(),
    ): Boolean {
        if (!isReady) return false
        val mode = if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }
        return tts.speak(text, mode, params, utteranceId) == TextToSpeech.SUCCESS
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
