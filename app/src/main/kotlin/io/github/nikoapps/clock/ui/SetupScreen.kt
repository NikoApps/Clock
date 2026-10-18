package io.github.nikoapps.clock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.VoiceStatus
import io.github.nikoapps.clock.health.HealthProblem
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.commons.ui.BigButton
import io.github.nikoapps.commons.ui.ScreenTopBar
import io.github.nikoapps.commons.ui.SecondaryButton
import io.github.nikoapps.commons.ui.SettingsGroup

private enum class SetupStep(val title: Int, val intro: Int) {
    Voice(R.string.section_voice, R.string.setup_voice_intro),
    Chimes(R.string.section_chimes, R.string.setup_chimes_intro),
    Shake(R.string.section_shake, R.string.setup_shake_intro),
    Pin(R.string.section_pin, R.string.setup_pin_intro),
    Phone(R.string.section_phone_setup, R.string.setup_phone_intro),
}

/**
 * First-run setup for the helper, one short step at a time, using the same rows as settings.
 * Nothing is required: Next moves on whatever is chosen, and Skip setup leaves at any point.
 * [step] is kept by the caller so it survives a trip to the PIN screen.
 */
@Composable
fun SetupScreen(
    step: Int,
    onStep: (Int) -> Unit,
    settings: Settings,
    voice: VoiceStatus,
    engine: String?,
    problems: List<HealthProblem>,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onTestVoice: () -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
    onNeedNotifications: () -> Unit,
    onFinish: () -> Unit,
) {
    val current = SetupStep.entries[step.coerceIn(0, SetupStep.entries.lastIndex)]
    val last = current == SetupStep.entries.last()
    val back = { if (step > 0) onStep(step - 1) else onFinish() }
    BackHandler(onBack = back)

    ProvideInfoStrings {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenTopBar(
                title = stringResource(R.string.setup_title),
                backIcon = painterResource(R.drawable.ic_arrow_back),
                backLabel = stringResource(R.string.back),
                onBack = back,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.setup_step, current.ordinal + 1, SetupStep.entries.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(current.intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                SettingsGroup(stringResource(current.title)) {
                    when (current) {
                        SetupStep.Voice -> {
                            VoiceLanguageRow(settings, onUpdate)
                            VoiceProblemRow(voice)
                            SpeechSpeedRow(settings, onUpdate)
                            TestVoiceRow(engine, onTestVoice)
                        }
                        SetupStep.Chimes -> ChimeRows(settings, onUpdate, onNeedNotifications)
                        SetupStep.Shake -> ShakeRows(settings, onUpdate, onNeedNotifications)
                        SetupStep.Pin -> PinRows(settings, onSetPin, onRemovePin)
                        SetupStep.Phone -> {
                            if (problems.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.setup_phone_ok),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                                )
                            } else {
                                HealthRows(problems)
                            }
                            PhoneMakerRow()
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BigButton(
                    text = stringResource(if (last) R.string.setup_done else R.string.setup_next),
                    onClick = { if (last) onFinish() else onStep(step + 1) },
                )
                if (!last) SecondaryButton(text = stringResource(R.string.setup_skip), onClick = onFinish)
            }
        }
    }
}
