package io.github.nikoapps.clock.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.commons.health.HealthCheck
import io.github.nikoapps.commons.speech.VoiceStatus
import io.github.nikoapps.commons.ui.HealthRows
import io.github.nikoapps.commons.ui.PhoneMakerRow
import io.github.nikoapps.commons.ui.PinRows
import io.github.nikoapps.commons.ui.SetupFrame
import io.github.nikoapps.commons.ui.SetupStep
import io.github.nikoapps.commons.ui.SpeechSpeedRow
import io.github.nikoapps.commons.ui.TestVoiceRow
import io.github.nikoapps.commons.ui.VoiceLanguageRow
import io.github.nikoapps.commons.ui.VoiceProblemRow
import io.github.nikoapps.commons.R as CommonsR

/**
 * First-run setup for the helper: voice, chimes, shake, PIN and phone. The frame with Next, Skip
 * and Back is shared with other Niko apps; these are Clock's steps.
 * [step] is kept by the caller so it survives a trip to the PIN screen.
 */
@Composable
fun SetupScreen(
    step: Int,
    onStep: (Int) -> Unit,
    settings: Settings,
    voice: VoiceStatus,
    engine: String?,
    problems: List<HealthCheck>,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onTestVoice: () -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
    onNeedNotifications: () -> Unit,
    onFinish: () -> Unit,
) {
    val steps = listOf(
        SetupStep(stringResource(R.string.section_voice)) {
            VoiceLanguageRow(VOICE_LANGUAGES, settings.language) { tag -> onUpdate { it.copy(language = tag) } }
            VoiceProblemRow(voice)
            SpeechSpeedRow(settings.speechSpeed) { speed -> onUpdate { it.copy(speechRate = speed.rate) } }
            TestVoiceRow(engine, onTestVoice)
        },
        SetupStep(stringResource(R.string.section_chimes)) {
            ChimeRows(settings, onUpdate, onNeedNotifications)
        },
        SetupStep(stringResource(R.string.section_shake)) {
            ShakeRows(settings, onUpdate, onNeedNotifications)
        },
        SetupStep(stringResource(CommonsR.string.section_pin)) {
            PinRows(settings.helperPinHash != null, stringResource(R.string.info_pin), onSetPin, onRemovePin)
        },
        SetupStep(stringResource(R.string.section_phone_setup)) {
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
            PhoneMakerRow(stringResource(R.string.info_battery))
        },
    )
    SetupFrame(steps = steps, step = step, onStep = onStep, onFinish = onFinish)
}
