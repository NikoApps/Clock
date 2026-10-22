package io.github.nikoapps.clock

import android.app.Application
import android.media.AudioAttributes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.clock.settings.SettingsRepository
import io.github.nikoapps.clock.speech.spokenDate
import io.github.nikoapps.clock.speech.TimePhraser
import io.github.nikoapps.clock.speech.phraserFor
import io.github.nikoapps.commons.speech.Speaker
import io.github.nikoapps.commons.speech.VoiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** [engine] is the speech engine's name, shown to helpers so they know which voice is in use. */
data class ClockState(val voice: VoiceStatus = VoiceStatus.Starting, val engine: String? = null)

class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private var phraser: TimePhraser? = null
    private var settings = Settings()

    private val _state = MutableStateFlow(ClockState())
    val state: StateFlow<ClockState> = _state.asStateFlow()

    // Someone pressing the button wants an answer now, so speak on the accessibility
    // stream, which Do Not Disturb never mutes.
    private val speaker = Speaker(
        context = application,
        audioUsage = AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY,
        onReady = { ready -> if (ready) checkVoice() else setVoice(VoiceStatus.NoEngine) },
    )

    init {
        // Language and speed come from helper settings and can change while the screen is open.
        viewModelScope.launch {
            SettingsRepository(application).settings.collect { settings ->
                this@ClockViewModel.settings = settings
                phraser = phraserFor(settings)
                speaker.setRate(settings.speechRate)
                speaker.setPitch(settings.voicePitch.pitch)
                if (phraser == null) setVoice(VoiceStatus.LanguageNotSupported) else checkVoice()
            }
        }
    }

    fun onSpeak() = speakWith { it.phrase(LocalTime.now()) }

    fun onSpeakDate() = speakWith { spokenDate(it, settings, LocalDate.now()) }

    private fun speakWith(sentence: (TimePhraser) -> String) {
        val phraser = phraser ?: return
        // A helper may have installed a voice since the last check.
        if (_state.value.voice != VoiceStatus.Ready) checkVoice()
        if (_state.value.voice == VoiceStatus.Ready) speaker.speak(sentence(phraser))
    }

    private fun checkVoice() {
        val phraser = phraser ?: return
        if (!speaker.isReady) return
        _state.update { it.copy(engine = speaker.engineName()) }
        setVoice(if (speaker.useVoiceFor(phraser.locale)) VoiceStatus.Ready else VoiceStatus.NoVoice)
    }

    private fun setVoice(status: VoiceStatus) = _state.update { it.copy(voice = status) }

    override fun onCleared() {
        speaker.shutdown()
    }
}
