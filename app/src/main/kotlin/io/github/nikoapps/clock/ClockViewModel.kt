package io.github.nikoapps.clock

import android.app.Application
import android.media.AudioAttributes
import androidx.lifecycle.AndroidViewModel
import io.github.nikoapps.clock.speech.IcuNumberWords
import io.github.nikoapps.clock.speech.timePhraserFor
import io.github.nikoapps.commons.speech.Speaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.util.Locale

enum class VoiceStatus {
    /** The speech engine is still starting. */
    Starting,
    Ready,

    /** The phone has no text-to-speech engine at all. */
    NoEngine,

    /** There's an engine, but no installed offline voice for the language. */
    NoVoice,

    /** Niko Clock has no phrases for the phone's language yet. */
    LanguageNotSupported,
}

data class ClockState(val voice: VoiceStatus = VoiceStatus.Starting)

class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val phraser = timePhraserFor(Locale.getDefault()) { IcuNumberWords(it) }

    private val _state = MutableStateFlow(
        ClockState(voice = if (phraser == null) VoiceStatus.LanguageNotSupported else VoiceStatus.Starting),
    )
    val state: StateFlow<ClockState> = _state.asStateFlow()

    // Someone pressing the button wants an answer now, so speak on the accessibility
    // stream, which Do Not Disturb never mutes.
    private val speaker = Speaker(
        context = application,
        audioUsage = AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY,
        onReady = { ready -> if (ready) checkVoice() else setVoice(VoiceStatus.NoEngine) },
    )

    fun onSpeak() {
        val phraser = phraser ?: return
        // A helper may have installed a voice since the last check.
        if (_state.value.voice != VoiceStatus.Ready) checkVoice()
        if (_state.value.voice == VoiceStatus.Ready) speaker.speak(phraser.phrase(LocalTime.now()))
    }

    private fun checkVoice() {
        val phraser = phraser ?: return
        if (!speaker.isReady) return
        setVoice(if (speaker.useVoiceFor(phraser.locale)) VoiceStatus.Ready else VoiceStatus.NoVoice)
    }

    private fun setVoice(status: VoiceStatus) = _state.update { it.copy(voice = status) }

    override fun onCleared() {
        speaker.shutdown()
    }
}
