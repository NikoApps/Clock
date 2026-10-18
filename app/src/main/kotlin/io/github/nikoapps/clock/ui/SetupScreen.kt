package io.github.nikoapps.clock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.VoiceStatus
import io.github.nikoapps.clock.health.HealthProblem
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.commons.ui.ScreenTopBar
import io.github.nikoapps.commons.ui.SecondaryButton
import io.github.nikoapps.commons.ui.SolidButton
import io.github.nikoapps.commons.ui.SettingsGroup

private enum class SetupStep(val title: Int) {
    Voice(R.string.section_voice),
    Chimes(R.string.section_chimes),
    Shake(R.string.section_shake),
    Pin(R.string.section_pin),
    Phone(R.string.section_phone_setup),
}

/**
 * First-run setup for the helper, one short step at a time, using the same rows as settings.
 * Nothing is required: Next moves on whatever is chosen, and Skip setup leaves at any point.
 * Next and Skip sit side by side in calm colours; the big accent button belongs to the main screen.
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
                // The rows explain themselves through their (i) buttons, so the step needs no intro text.
                Text(
                    text = stringResource(R.string.setup_step, current.ordinal + 1, SetupStep.entries.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Read out when Next or Back changes the step, since the title stays the same.
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
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
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!last) {
                    SecondaryButton(
                        text = stringResource(R.string.setup_skip),
                        onClick = onFinish,
                        modifier = Modifier.weight(1f),
                    )
                }
                SolidButton(
                    text = stringResource(if (last) R.string.setup_done else R.string.setup_next),
                    onClick = { if (last) onFinish() else onStep(step + 1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
