package io.github.nikoapps.clock.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.VoiceStatus
import io.github.nikoapps.clock.health.HealthProblem
import io.github.nikoapps.clock.health.PhoneHealth
import io.github.nikoapps.clock.health.PhoneMaker
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.clock.settings.ShakeSensitivity
import io.github.nikoapps.clock.settings.SpeechSpeed
import io.github.nikoapps.clock.settings.TextSize
import io.github.nikoapps.clock.settings.VoicePitch
import io.github.nikoapps.commons.ui.ChoiceRow
import io.github.nikoapps.commons.ui.InfoStrings
import io.github.nikoapps.commons.ui.LocalInfoStrings
import io.github.nikoapps.commons.ui.SwitchRow
import io.github.nikoapps.commons.ui.ValueRow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Rows shared by the settings screen and first-run setup, so both always look and behave the same.

/** Languages Niko Clock can speak the time in. Each has hand-written phrases. */
private val VOICE_LANGUAGES: List<String?> = listOf(null, "en", "ne-NP", "hi-IN", "bn", "ur", "es", "pt-BR", "fr", "id")

/** Each language in its own words, so people can find theirs without reading another. */
private val LANGUAGE_NAMES = mapOf(
    "en" to "English",
    "ne" to "नेपाली",
    "hi" to "हिन्दी",
    "bn" to "বাংলা",
    "ur" to "اردو",
    "ar" to "العربية",
    "es" to "Español",
    "pt" to "Português",
    "fr" to "Français",
    // Android still calls Indonesian "in"; newer code says "id".
    "in" to "Bahasa Indonesia",
    "id" to "Bahasa Indonesia",
)
private val CHIME_INTERVALS = listOf(60, 30, 15)
private val CHIME_VOLUMES = listOf(50, 80, 100)
private val QUIET_STARTS = listOf(LocalTime.of(21, 0), LocalTime.of(22, 0), LocalTime.of(23, 0))
private val QUIET_ENDS = listOf(LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0))

/** Opens another app's page, or the phone's settings if this phone doesn't have that page. */
fun openPage(context: Context, intent: Intent, fallback: Intent = Intent(AndroidSettings.ACTION_SETTINGS)) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: SecurityException) {
        context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Gives rows below it their (i) buttons. */
@Composable
fun ProvideInfoStrings(content: @Composable () -> Unit) {
    val resources = LocalResources.current
    val infoStrings = InfoStrings(
        icon = painterResource(R.drawable.ic_info),
        moreAbout = { title -> resources.getString(R.string.info_more_about, title) },
        ok = stringResource(R.string.ok),
    )
    CompositionLocalProvider(LocalInfoStrings provides infoStrings, content = content)
}

@Composable
fun languageName(tag: String?): String =
    tag?.let { LANGUAGE_NAMES[it.substringBefore('-')] } ?: stringResource(R.string.language_phone)

@Composable
fun VoiceLanguageRow(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    ChoiceRow(
        title = stringResource(R.string.setting_voice_language),
        options = VOICE_LANGUAGES,
        selected = VOICE_LANGUAGES.firstOrNull { it == settings.language },
        optionLabel = { languageName(it) },
        onSelect = { tag -> onUpdate { it.copy(language = tag) } },
        cancelLabel = stringResource(R.string.cancel),
    )
}

/** Only shown when something is wrong, and it says how to fix it. */
@Composable
fun VoiceProblemRow(voice: VoiceStatus) {
    if (voice == VoiceStatus.Ready || voice == VoiceStatus.Starting) return
    val context = LocalContext.current
    ValueRow(
        title = stringResource(R.string.voice_problem),
        value = stringResource(voiceMessage(voice)),
        valueIsWarning = true,
        trailing = painterResource(R.drawable.ic_chevron_right),
        onClick = { openPage(context, Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) },
    )
}

@Composable
fun SpeechSpeedRow(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    ChoiceRow(
        title = stringResource(R.string.setting_speech_speed),
        options = SpeechSpeed.entries,
        selected = settings.speechSpeed,
        optionLabel = { stringResource(it.label()) },
        onSelect = { speed -> onUpdate { it.copy(speechRate = speed.rate) } },
        cancelLabel = stringResource(R.string.cancel),
    )
}

/** "Test the voice", with the speech engine's name under it so a helper knows which voice is in use. */
@Composable
fun TestVoiceRow(engine: String?, onTestVoice: () -> Unit) {
    ValueRow(
        title = stringResource(R.string.test_voice),
        value = engine?.let { stringResource(R.string.voice_engine, it) },
        onClick = onTestVoice,
    )
}

/** The chimes switch, and the rows that only matter while it's on. */
@Composable
fun ChimeRows(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, onNeedNotifications: () -> Unit) {
    val cancel = stringResource(R.string.cancel)
    SwitchRow(
        title = stringResource(R.string.setting_chime),
        info = stringResource(R.string.info_chime),
        checked = settings.chimeEnabled,
        onCheckedChange = { checked ->
            if (checked) onNeedNotifications()
            onUpdate { it.copy(chimeEnabled = checked) }
        },
    )
    if (!settings.chimeEnabled) return
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
        info = stringResource(R.string.info_quiet_hours),
        value = if (settings.quietHoursEnabled) {
            "${clockTime(settings.quietStart)} – ${clockTime(settings.quietEnd)}"
        } else {
            stringResource(R.string.value_off)
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
        info = stringResource(R.string.info_dnd),
        checked = settings.chimeFollowsDoNotDisturb,
        onCheckedChange = { checked -> onUpdate { it.copy(chimeFollowsDoNotDisturb = checked) } },
    )
}

/** The shake switch, and the rows that only matter while it's on. */
@Composable
fun ShakeRows(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, onNeedNotifications: () -> Unit) {
    SwitchRow(
        title = stringResource(R.string.setting_shake),
        info = stringResource(R.string.info_shake),
        value = stringResource(R.string.setting_shake_value),
        checked = settings.shakeEnabled,
        onCheckedChange = { checked ->
            if (checked) onNeedNotifications()
            onUpdate { it.copy(shakeEnabled = checked) }
        },
    )
    if (!settings.shakeEnabled) return
    SwitchRow(
        title = stringResource(R.string.setting_shake_date),
        info = stringResource(R.string.info_shake_date),
        checked = settings.shakeAgainForDate,
        onCheckedChange = { checked -> onUpdate { it.copy(shakeAgainForDate = checked) } },
    )
    ChoiceRow(
        title = stringResource(R.string.setting_shake_sensitivity),
        options = listOf(ShakeSensitivity.High, ShakeSensitivity.Medium, ShakeSensitivity.Low),
        selected = settings.shakeSensitivity,
        optionLabel = { stringResource(it.label()) },
        onSelect = { s -> onUpdate { it.copy(shakeSensitivity = s) } },
        cancelLabel = stringResource(R.string.cancel),
    )
}

@Composable
fun PinRows(settings: Settings, onSetPin: () -> Unit, onRemovePin: () -> Unit) {
    ValueRow(
        title = stringResource(R.string.section_pin),
        info = stringResource(R.string.info_pin),
        value = stringResource(if (settings.helperPinHash == null) R.string.value_off else R.string.value_on),
        trailing = painterResource(R.drawable.ic_chevron_right),
        onClick = onSetPin,
    )
    if (settings.helperPinHash != null) {
        ValueRow(title = stringResource(R.string.pin_remove), value = null, onClick = onRemovePin)
    }
}

/** One row per phone setting that can silence Niko Clock, each opening the page that fixes it. */
@Composable
fun HealthRows(problems: List<HealthProblem>) {
    val context = LocalContext.current
    val chevron = painterResource(R.drawable.ic_chevron_right)
    problems.forEach { problem ->
        val (title, value) = when (problem) {
            HealthProblem.ExactAlarms -> R.string.health_exact_alarms to R.string.health_exact_alarms_value
            HealthProblem.Battery -> R.string.health_battery to R.string.health_battery_value
            HealthProblem.Hibernation -> R.string.health_hibernation to R.string.health_hibernation_value
        }
        ValueRow(
            title = stringResource(title),
            value = stringResource(value),
            valueIsWarning = true,
            trailing = chevron,
            onClick = { openPage(context, PhoneHealth.fixIntent(context, problem), PhoneHealth.appInfo(context)) },
        )
    }
}

/**
 * The phone maker's own battery and autostart settings. Android can't tell us whether these are
 * set, so this row is always shown, with steps for the maker of this phone.
 */
@Composable
fun PhoneMakerRow() {
    val context = LocalContext.current
    val maker = PhoneMaker.current()
    ValueRow(
        title = stringResource(R.string.phone_maker),
        info = stringResource(R.string.info_battery),
        value = stringResource(
            when (maker) {
                PhoneMaker.Xiaomi -> R.string.phone_maker_xiaomi
                PhoneMaker.Samsung -> R.string.phone_maker_samsung
                PhoneMaker.Other -> R.string.phone_maker_other
            },
        ),
        trailing = painterResource(R.drawable.ic_chevron_right),
        onClick = { openPage(context, maker.settingsIntent(context), PhoneHealth.appInfo(context)) },
    )
}

/** "9:00 PM" or "21:00", following the phone's clock style. */
fun clockTime(time: LocalTime): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

fun SpeechSpeed.label(): Int = when (this) {
    SpeechSpeed.Slow -> R.string.speed_slow
    SpeechSpeed.Normal -> R.string.speed_normal
    SpeechSpeed.Fast -> R.string.speed_fast
}

fun TextSize.label(): Int = when (this) {
    TextSize.Small -> R.string.size_small
    TextSize.Medium -> R.string.size_medium
    TextSize.Large -> R.string.size_large
}

fun VoicePitch.label(): Int = when (this) {
    VoicePitch.Lower -> R.string.pitch_lower
    VoicePitch.Normal -> R.string.pitch_normal
    VoicePitch.Higher -> R.string.pitch_higher
}

fun ShakeSensitivity.label(): Int = when (this) {
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
