package io.github.nikoapps.clock.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.VoiceStatus
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.clock.settings.ShakeSensitivity
import io.github.nikoapps.clock.settings.SpeechSpeed
import io.github.nikoapps.clock.settings.TextSize
import io.github.nikoapps.commons.ui.BigButton
import io.github.nikoapps.commons.ui.ChoiceGroup
import io.github.nikoapps.commons.ui.Explanation
import io.github.nikoapps.commons.ui.SecondaryButton
import io.github.nikoapps.commons.ui.SectionTitle
import io.github.nikoapps.commons.ui.SwitchRow
import java.time.LocalTime

/** Language choices: the phone's own language, or one Niko Clock can speak. */
private val LANGUAGES: List<String?> = listOf(null, "en", "ne-NP")
private val CHIME_INTERVALS = listOf(60, 30, 15)
private val CHIME_VOLUMES = listOf(50, 80, 100)
private val QUIET_STARTS = listOf(LocalTime.of(21, 0), LocalTime.of(22, 0), LocalTime.of(23, 0))
private val QUIET_ENDS = listOf(LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0))

/**
 * Settings for helpers. Every setting is an on/off switch or a choice of three plain options.
 * This screen may scroll; the main clock screen never does.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    voice: VoiceStatus,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onLanguage: (String?) -> Unit,
    onTestVoice: () -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
    onNeedNotifications: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    fun open(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(AndroidSettings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        SecondaryButton(text = stringResource(R.string.back_to_clock), onClick = onBack)

        // Voice
        SectionTitle(stringResource(R.string.section_voice))
        ChoiceGroup(
            label = stringResource(R.string.setting_language),
            options = LANGUAGES,
            selected = settings.language?.let { tag -> LANGUAGES.firstOrNull { it != null && tag.startsWith(it.substringBefore('-')) } },
            optionLabel = { tag ->
                when (tag) {
                    null -> stringResource(R.string.language_phone)
                    "en" -> "English"
                    else -> "नेपाली"
                }
            },
            onSelect = onLanguage,
        )
        Explanation(stringResource(voiceMessage(voice)))
        BigButton(text = stringResource(R.string.test_voice), onClick = onTestVoice)
        SecondaryButton(
            text = stringResource(R.string.install_voice),
            onClick = { open(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) },
        )
        ChoiceGroup(
            label = stringResource(R.string.setting_speech_speed),
            options = SpeechSpeed.entries,
            selected = settings.speechSpeed,
            optionLabel = { stringResource(it.label()) },
            onSelect = { speed -> onUpdate { it.copy(speechRate = speed.rate) } },
        )
        SwitchRow(
            label = stringResource(R.string.setting_vibrate),
            explanation = stringResource(R.string.setting_vibrate_explanation),
            checked = settings.vibrateBeforeSpeaking,
            onCheckedChange = { on -> onUpdate { it.copy(vibrateBeforeSpeaking = on) } },
        )

        // Screen
        SectionTitle(stringResource(R.string.section_screen))
        ChoiceGroup(
            label = stringResource(R.string.setting_text_size),
            options = TextSize.entries,
            selected = settings.textSize,
            optionLabel = { stringResource(it.label()) },
            onSelect = { size -> onUpdate { it.copy(textSize = size) } },
        )

        // Chimes
        SectionTitle(stringResource(R.string.section_chimes))
        SwitchRow(
            label = stringResource(R.string.setting_chime),
            explanation = stringResource(R.string.setting_chime_explanation),
            checked = settings.chimeEnabled,
            onCheckedChange = { on ->
                if (on) onNeedNotifications()
                onUpdate { it.copy(chimeEnabled = on) }
            },
        )
        ChoiceGroup(
            label = stringResource(R.string.setting_chime_interval),
            options = CHIME_INTERVALS,
            selected = settings.chimeIntervalMinutes,
            optionLabel = { minutes ->
                stringResource(
                    when (minutes) {
                        60 -> R.string.chime_every_hour
                        30 -> R.string.chime_every_half_hour
                        else -> R.string.chime_every_quarter_hour
                    },
                )
            },
            onSelect = { minutes -> onUpdate { it.copy(chimeIntervalMinutes = minutes) } },
        )
        ChoiceGroup(
            label = stringResource(R.string.setting_chime_volume),
            options = CHIME_VOLUMES,
            selected = CHIME_VOLUMES.minBy { kotlin.math.abs(it - settings.chimeVolumePercent) },
            optionLabel = { percent ->
                stringResource(
                    when (percent) {
                        50 -> R.string.volume_quiet
                        80 -> R.string.volume_medium
                        else -> R.string.volume_loud
                    },
                )
            },
            onSelect = { percent -> onUpdate { it.copy(chimeVolumePercent = percent) } },
        )
        SwitchRow(
            label = stringResource(R.string.setting_follow_dnd),
            explanation = stringResource(R.string.setting_follow_dnd_explanation),
            checked = settings.chimeFollowsDoNotDisturb,
            onCheckedChange = { on -> onUpdate { it.copy(chimeFollowsDoNotDisturb = on) } },
        )

        // Quiet hours
        SectionTitle(stringResource(R.string.section_quiet_hours))
        SwitchRow(
            label = stringResource(R.string.setting_quiet_hours),
            explanation = stringResource(R.string.setting_quiet_hours_explanation),
            checked = settings.quietHoursEnabled,
            onCheckedChange = { on -> onUpdate { it.copy(quietHoursEnabled = on) } },
        )
        ChoiceGroup(
            label = stringResource(R.string.setting_quiet_start),
            options = QUIET_STARTS,
            selected = QUIET_STARTS.firstOrNull { it == settings.quietStart } ?: QUIET_STARTS[1],
            optionLabel = { stringResource(R.string.quiet_start_at, it.hour - 12) },
            onSelect = { time -> onUpdate { it.copy(quietStart = time) } },
        )
        ChoiceGroup(
            label = stringResource(R.string.setting_quiet_end),
            options = QUIET_ENDS,
            selected = QUIET_ENDS.firstOrNull { it == settings.quietEnd } ?: QUIET_ENDS[1],
            optionLabel = { stringResource(R.string.quiet_end_at, it.hour) },
            onSelect = { time -> onUpdate { it.copy(quietEnd = time) } },
        )

        // Shake
        SectionTitle(stringResource(R.string.section_shake))
        SwitchRow(
            label = stringResource(R.string.setting_shake),
            explanation = stringResource(R.string.setting_shake_explanation),
            checked = settings.shakeEnabled,
            onCheckedChange = { on ->
                if (on) onNeedNotifications()
                onUpdate { it.copy(shakeEnabled = on) }
            },
        )
        ChoiceGroup(
            label = stringResource(R.string.setting_shake_sensitivity),
            options = ShakeSensitivity.entries,
            selected = settings.shakeSensitivity,
            optionLabel = { stringResource(it.label()) },
            onSelect = { s -> onUpdate { it.copy(shakeSensitivity = s) } },
        )

        // Keep it working
        SectionTitle(stringResource(R.string.section_phone_setup))
        Explanation(stringResource(R.string.phone_setup_explanation))
        SecondaryButton(
            text = stringResource(R.string.open_battery_settings),
            onClick = { open(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
        )

        // Helper PIN
        SectionTitle(stringResource(R.string.section_pin))
        Explanation(stringResource(if (settings.helperPinHash == null) R.string.pin_none else R.string.pin_is_set))
        SecondaryButton(
            text = stringResource(if (settings.helperPinHash == null) R.string.pin_set else R.string.pin_change),
            onClick = onSetPin,
        )
        if (settings.helperPinHash != null) {
            SecondaryButton(text = stringResource(R.string.pin_remove), onClick = onRemovePin)
        }

        SecondaryButton(
            text = stringResource(R.string.back_to_clock),
            onClick = onBack,
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}

private fun SpeechSpeed.label(): Int = when (this) {
    SpeechSpeed.Slow -> R.string.speed_slow
    SpeechSpeed.Normal -> R.string.speed_normal
    SpeechSpeed.Fast -> R.string.speed_fast
}

private fun TextSize.label(): Int = when (this) {
    TextSize.Small -> R.string.size_small
    TextSize.Medium -> R.string.size_medium
    TextSize.Large -> R.string.size_large
}

private fun ShakeSensitivity.label(): Int = when (this) {
    ShakeSensitivity.Low -> R.string.sensitivity_low
    ShakeSensitivity.Medium -> R.string.sensitivity_medium
    ShakeSensitivity.High -> R.string.sensitivity_high
}

fun voiceMessage(voice: VoiceStatus): Int = when (voice) {
    VoiceStatus.Starting, VoiceStatus.Ready -> R.string.status_ready
    VoiceStatus.NoVoice -> R.string.status_no_voice
    VoiceStatus.NoEngine -> R.string.status_no_engine
    VoiceStatus.LanguageNotSupported -> R.string.status_language_not_supported
}
