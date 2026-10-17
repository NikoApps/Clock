package io.github.nikoapps.clock.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.VoiceStatus
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.clock.settings.ShakeSensitivity
import io.github.nikoapps.clock.settings.SpeechSpeed
import io.github.nikoapps.clock.settings.TextSize
import io.github.nikoapps.commons.ui.ChoiceRow
import io.github.nikoapps.commons.ui.ScreenTopBar
import io.github.nikoapps.commons.ui.SettingsGroup
import io.github.nikoapps.commons.ui.SwitchRow
import io.github.nikoapps.commons.ui.ValueRow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Language choices: the phone's own language, or one Niko Clock has. */
private val VOICE_LANGUAGES: List<String?> = listOf(null, "en", "ne-NP")
private val SCREEN_LANGUAGES: List<String?> = listOf(null, "en", "ne")
private val CHIME_INTERVALS = listOf(60, 30, 15)
private val CHIME_VOLUMES = listOf(50, 80, 100)
private val QUIET_STARTS = listOf(LocalTime.of(21, 0), LocalTime.of(22, 0), LocalTime.of(23, 0))
private val QUIET_ENDS = listOf(LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0))

/**
 * Settings for helpers, as a plain list like Android's own settings. The title bar stays put
 * while the list scrolls. Each row shows its current value; choices open a dialog of three
 * options. Rows that only matter when a switch is on are shown only then.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    voice: VoiceStatus,
    screenLanguage: String?,
    canChooseScreenLanguage: Boolean,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onScreenLanguage: (String?) -> Unit,
    onTestVoice: () -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
    onNeedNotifications: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val chevron = painterResource(R.drawable.ic_chevron_right)
    fun open(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(AndroidSettings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    @Composable
    fun languageName(tag: String?): String = when (tag?.substringBefore('-')) {
        null -> stringResource(R.string.language_phone)
        "en" -> "English"
        else -> "नेपाली"
    }

    val cancel = stringResource(R.string.cancel)
    val on = stringResource(R.string.value_on)
    val off = stringResource(R.string.value_off)

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenTopBar(
            title = stringResource(R.string.settings_title),
            backIcon = painterResource(R.drawable.ic_arrow_back),
            backLabel = stringResource(R.string.back),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            SettingsGroup(stringResource(R.string.section_voice)) {
                val voiceProblem = voice != VoiceStatus.Ready && voice != VoiceStatus.Starting
                ChoiceRow(
                    title = stringResource(R.string.setting_voice_language),
                    options = VOICE_LANGUAGES,
                    selected = VOICE_LANGUAGES.firstOrNull { it == settings.language },
                    optionLabel = { languageName(it) },
                    onSelect = { tag -> onUpdate { it.copy(language = tag) } },
                    cancelLabel = cancel,
                )
                if (voiceProblem) {
                    // Only shown when something is wrong, and it says how to fix it.
                    ValueRow(
                        title = stringResource(R.string.voice_problem),
                        value = stringResource(voiceMessage(voice)),
                        valueIsWarning = true,
                        trailing = chevron,
                        onClick = { open(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) },
                    )
                }
                ChoiceRow(
                    title = stringResource(R.string.setting_speech_speed),
                    options = SpeechSpeed.entries,
                    selected = settings.speechSpeed,
                    optionLabel = { stringResource(it.label()) },
                    onSelect = { speed -> onUpdate { it.copy(speechRate = speed.rate) } },
                    cancelLabel = cancel,
                )
                SwitchRow(
                    title = stringResource(R.string.setting_vibrate),
                    checked = settings.vibrateBeforeSpeaking,
                    onCheckedChange = { checked -> onUpdate { it.copy(vibrateBeforeSpeaking = checked) } },
                )
                ValueRow(title = stringResource(R.string.test_voice), value = null, onClick = onTestVoice)
                ValueRow(
                    title = stringResource(R.string.install_voice),
                    value = null,
                    trailing = chevron,
                    onClick = { open(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) },
                )
            }

            SettingsGroup(stringResource(R.string.section_chimes)) {
                SwitchRow(
                    title = stringResource(R.string.setting_chime),
                    checked = settings.chimeEnabled,
                    onCheckedChange = { checked ->
                        if (checked) onNeedNotifications()
                        onUpdate { it.copy(chimeEnabled = checked) }
                    },
                )
                if (settings.chimeEnabled) {
                    ChoiceRow(
                        title = stringResource(R.string.setting_chime_interval),
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
                        cancelLabel = cancel,
                    )
                    ChoiceRow(
                        title = stringResource(R.string.setting_chime_volume),
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
                        cancelLabel = cancel,
                    )
                    SwitchRow(
                        title = stringResource(R.string.setting_quiet_hours),
                        value = if (settings.quietHoursEnabled) {
                            "${clockTime(settings.quietStart)} – ${clockTime(settings.quietEnd)}"
                        } else {
                            off
                        },
                        checked = settings.quietHoursEnabled,
                        onCheckedChange = { checked -> onUpdate { it.copy(quietHoursEnabled = checked) } },
                    )
                    if (settings.quietHoursEnabled) {
                        ChoiceRow(
                            title = stringResource(R.string.setting_quiet_start),
                            options = QUIET_STARTS,
                            selected = QUIET_STARTS.firstOrNull { it == settings.quietStart } ?: QUIET_STARTS[1],
                            optionLabel = { clockTime(it) },
                            onSelect = { time -> onUpdate { it.copy(quietStart = time) } },
                            cancelLabel = cancel,
                        )
                        ChoiceRow(
                            title = stringResource(R.string.setting_quiet_end),
                            options = QUIET_ENDS,
                            selected = QUIET_ENDS.firstOrNull { it == settings.quietEnd } ?: QUIET_ENDS[1],
                            optionLabel = { clockTime(it) },
                            onSelect = { time -> onUpdate { it.copy(quietEnd = time) } },
                            cancelLabel = cancel,
                        )
                    }
                    SwitchRow(
                        title = stringResource(R.string.setting_follow_dnd),
                        checked = settings.chimeFollowsDoNotDisturb,
                        onCheckedChange = { checked -> onUpdate { it.copy(chimeFollowsDoNotDisturb = checked) } },
                    )
                }
            }

            SettingsGroup(stringResource(R.string.section_shake)) {
                SwitchRow(
                    title = stringResource(R.string.setting_shake),
                    value = stringResource(R.string.setting_shake_value),
                    checked = settings.shakeEnabled,
                    onCheckedChange = { checked ->
                        if (checked) onNeedNotifications()
                        onUpdate { it.copy(shakeEnabled = checked) }
                    },
                )
                if (settings.shakeEnabled) {
                    ChoiceRow(
                        title = stringResource(R.string.setting_shake_sensitivity),
                        options = listOf(ShakeSensitivity.High, ShakeSensitivity.Medium, ShakeSensitivity.Low),
                        selected = settings.shakeSensitivity,
                        optionLabel = { stringResource(it.label()) },
                        onSelect = { s -> onUpdate { it.copy(shakeSensitivity = s) } },
                        cancelLabel = cancel,
                    )
                }
            }

            SettingsGroup(stringResource(R.string.section_screen)) {
                if (canChooseScreenLanguage) {
                    ChoiceRow(
                        title = stringResource(R.string.setting_screen_language),
                        options = SCREEN_LANGUAGES,
                        selected = SCREEN_LANGUAGES.firstOrNull { it == screenLanguage },
                        optionLabel = { languageName(it) },
                        onSelect = onScreenLanguage,
                        cancelLabel = cancel,
                    )
                }
                ChoiceRow(
                    title = stringResource(R.string.setting_text_size),
                    options = TextSize.entries,
                    selected = settings.textSize,
                    optionLabel = { stringResource(it.label()) },
                    onSelect = { size -> onUpdate { it.copy(textSize = size) } },
                    cancelLabel = cancel,
                )
            }

            SettingsGroup(stringResource(R.string.section_phone_setup)) {
                ValueRow(
                    title = stringResource(R.string.open_battery_settings),
                    value = stringResource(R.string.battery_value),
                    trailing = chevron,
                    onClick = { open(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                )
                ValueRow(
                    title = stringResource(R.string.section_pin),
                    value = if (settings.helperPinHash == null) off else on,
                    trailing = chevron,
                    onClick = onSetPin,
                )
                if (settings.helperPinHash != null) {
                    ValueRow(title = stringResource(R.string.pin_remove), value = null, onClick = onRemovePin)
                }
            }
        }
    }
}

/** "9:00 PM" or "21:00", following the phone's clock style. */
private fun clockTime(time: LocalTime): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

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
