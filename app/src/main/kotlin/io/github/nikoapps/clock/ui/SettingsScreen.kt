package io.github.nikoapps.clock.ui

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.VoiceStatus
import io.github.nikoapps.clock.health.HealthProblem
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.clock.settings.TextSize
import io.github.nikoapps.clock.settings.VoicePitch
import io.github.nikoapps.commons.ui.ChoiceRow
import io.github.nikoapps.commons.ui.ScreenTopBar
import io.github.nikoapps.commons.ui.SettingsGroup
import io.github.nikoapps.commons.ui.SwitchRow
import io.github.nikoapps.commons.ui.ValueRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Languages the screen text is translated into. */
private val SCREEN_LANGUAGES: List<String?> = listOf(null, "en", "ne", "hi", "bn", "ur", "ar", "es", "pt", "fr", "in")

/**
 * Settings for helpers, as a plain list like Android's own settings. The title bar stays put
 * while the list scrolls. Each row shows its current value; choices open a dialog of three
 * options. Rows that only matter when a switch is on are shown only then.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    voice: VoiceStatus,
    engine: String?,
    problems: List<HealthProblem>,
    lastChimeAt: Instant?,
    screenLanguage: String?,
    canChooseScreenLanguage: Boolean,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onScreenLanguage: (String?) -> Unit,
    onTestVoice: () -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
    onNeedNotifications: () -> Unit,
    onStartSetup: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val chevron = painterResource(R.drawable.ic_chevron_right)
    val external = painterResource(R.drawable.ic_open_external)
    fun open(intent: Intent) = openPage(context, intent)

    val cancel = stringResource(R.string.cancel)

    ProvideInfoStrings {
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
            if (problems.isNotEmpty()) {
                SettingsGroup(stringResource(R.string.section_needs_attention)) {
                    HealthRows(problems)
                }
            }

            SettingsGroup(stringResource(R.string.section_voice)) {
                VoiceLanguageRow(settings, onUpdate)
                VoiceProblemRow(voice)
                SpeechSpeedRow(settings, onUpdate)
                ChoiceRow(
                    title = stringResource(R.string.setting_voice_pitch),
                    options = VoicePitch.entries,
                    selected = settings.voicePitch,
                    optionLabel = { stringResource(it.label()) },
                    onSelect = { pitch -> onUpdate { it.copy(voicePitch = pitch) } },
                    cancelLabel = cancel,
                )
                SwitchRow(
                    title = stringResource(R.string.setting_vibrate),
                    info = stringResource(R.string.info_vibrate),
                    checked = settings.vibrateBeforeSpeaking,
                    onCheckedChange = { checked -> onUpdate { it.copy(vibrateBeforeSpeaking = checked) } },
                )
                TestVoiceRow(engine, onTestVoice)
                ValueRow(
                    title = stringResource(R.string.install_voice),
                    value = null,
                    trailing = chevron,
                    onClick = { open(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) },
                )
            }

            SettingsGroup(stringResource(R.string.section_chimes)) {
                ChimeRows(settings, onUpdate, onNeedNotifications)
                if (settings.chimeEnabled) {
                    ValueRow(
                        title = stringResource(R.string.last_chime),
                        info = stringResource(R.string.info_last_chime),
                        value = lastChimeText(lastChimeAt),
                        onClick = null,
                    )
                }
            }

            SettingsGroup(stringResource(R.string.section_shake)) {
                ShakeRows(settings, onUpdate, onNeedNotifications)
            }

            SettingsGroup(stringResource(R.string.section_day_and_date)) {
                DayAndDateRows(settings, onUpdate, onNeedNotifications)
            }

            SettingsGroup(stringResource(R.string.section_day_clock)) {
                DayClockRows(settings, onUpdate)
            }

            // Screen language and text size apply to the whole app, in both clock layouts.
            SettingsGroup(stringResource(R.string.section_screen)) {
                if (canChooseScreenLanguage) {
                    ChoiceRow(
                        title = stringResource(R.string.setting_screen_language),
                        options = SCREEN_LANGUAGES,
                        selected = SCREEN_LANGUAGES.firstOrNull { it == screenLanguage?.replace("id", "in") },
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
                PhoneMakerRow()
                PinRows(settings, onSetPin, onRemovePin)
                ValueRow(
                    title = stringResource(R.string.setup_again),
                    value = stringResource(R.string.setup_again_value),
                    trailing = chevron,
                    onClick = onStartSetup,
                )
            }

            SettingsGroup(stringResource(R.string.section_support)) {
                ValueRow(
                    title = stringResource(R.string.help_feedback),
                    value = stringResource(R.string.help_feedback_value),
                    leading = painterResource(R.drawable.ic_help),
                    trailing = external,
                    onClick = { open(Intent(Intent.ACTION_VIEW, ISSUES_URL.toUri())) },
                )
                val shareText = stringResource(R.string.share_text, WEBSITE_URL)
                ValueRow(
                    title = stringResource(R.string.share_app),
                    value = stringResource(R.string.share_app_value),
                    leading = painterResource(R.drawable.ic_share),
                    trailing = chevron,
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                        open(Intent.createChooser(send, null))
                    },
                )
            }

            SettingsGroup(stringResource(R.string.section_about)) {
                var privacyOpen by rememberSaveable { mutableStateOf(false) }
                ValueRow(
                    title = stringResource(R.string.privacy),
                    value = stringResource(R.string.privacy_value),
                    leading = painterResource(R.drawable.ic_privacy),
                    trailing = chevron,
                    onClick = { privacyOpen = true },
                )
                if (privacyOpen) {
                    AlertDialog(
                        onDismissRequest = { privacyOpen = false },
                        title = { Text(stringResource(R.string.privacy), style = MaterialTheme.typography.titleLarge) },
                        text = { Text(stringResource(R.string.privacy_text), style = MaterialTheme.typography.bodyLarge) },
                        confirmButton = {
                            TextButton(onClick = { privacyOpen = false }, modifier = Modifier.heightIn(min = 56.dp)) {
                                Text(stringResource(R.string.ok), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                }
                ValueRow(
                    title = stringResource(R.string.source_code),
                    value = stringResource(R.string.source_code_value),
                    leading = painterResource(R.drawable.ic_code),
                    trailing = external,
                    onClick = { open(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri())) },
                )
                ValueRow(
                    title = stringResource(R.string.licence),
                    value = stringResource(R.string.licence_value),
                    leading = painterResource(R.drawable.ic_licence),
                    trailing = external,
                    onClick = { open(Intent(Intent.ACTION_VIEW, LICENCE_URL.toUri())) },
                )
                ValueRow(
                    title = stringResource(R.string.version),
                    value = appVersion(context),
                    leading = painterResource(R.drawable.ic_info),
                    onClick = null,
                )
            }
        }
    }
    }
}

private const val SOURCE_URL = "https://github.com/NikoApps/Clock"
private const val ISSUES_URL = "https://github.com/NikoApps/Clock/issues"
private const val WEBSITE_URL = "https://nikoapps.github.io"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/** The installed version name, read from the package so nothing is baked into the build. */
private fun appVersion(context: android.content.Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

/** "Today, 2:00 PM", "Yesterday, 9:00 PM", or the full date for anything older. */
@Composable
private fun lastChimeText(at: Instant?): String {
    if (at == null) return stringResource(R.string.last_chime_none)
    val local = at.atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val time = clockTime(local.toLocalTime())
    return when (local.toLocalDate()) {
        today -> stringResource(R.string.last_chime_today, time)
        today.minusDays(1) -> stringResource(R.string.last_chime_yesterday, time)
        else -> local.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }
}
