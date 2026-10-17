package io.github.nikoapps.clock

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nikoapps.clock.settings.HelperPin
import io.github.nikoapps.clock.settings.TextSize
import io.github.nikoapps.clock.ui.SetPinScreen
import io.github.nikoapps.clock.ui.SettingsScreen
import io.github.nikoapps.clock.ui.SettingsViewModel
import io.github.nikoapps.clock.ui.UnlockPinScreen
import io.github.nikoapps.clock.ui.voiceMessage
import io.github.nikoapps.commons.theme.NikoTheme
import io.github.nikoapps.commons.ui.BigButton
import io.github.nikoapps.commons.ui.SecondaryButton
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.lifecycle.lifecycleScope
import io.github.nikoapps.clock.settings.BackgroundSync
import io.github.nikoapps.clock.settings.SettingsRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            NikoTheme {
                TextSizeScale(settings?.textSize ?: TextSize.Medium) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        App(settingsViewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Opening the app brings background work back if Android or a battery saver stopped it.
        lifecycleScope.launch {
            BackgroundSync.sync(this@MainActivity, SettingsRepository(this@MainActivity).current())
        }
    }
}

private enum class Screen { Clock, Unlock, Settings, SetPin }

@Composable
private fun App(settingsViewModel: SettingsViewModel, clockViewModel: ClockViewModel = viewModel()) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val clock by clockViewModel.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Clock) }

    // Chimes and shake show a notification while they run; Android 13+ asks first.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val askForNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = screen != Screen.Clock) {
        screen = if (screen == Screen.SetPin) Screen.Settings else Screen.Clock
    }

    val current = settings ?: return
    when (screen) {
        Screen.Clock -> ClockScreen(
            voice = clock.voice,
            onSpeak = clockViewModel::onSpeak,
            onOpenSettings = { screen = if (current.helperPinHash == null) Screen.Settings else Screen.Unlock },
        )
        Screen.Unlock -> UnlockPinScreen(
            pinHash = current.helperPinHash ?: "",
            onUnlocked = { screen = Screen.Settings },
            onCancel = { screen = Screen.Clock },
        )
        Screen.Settings -> SettingsScreen(
            settings = current,
            voice = clock.voice,
            onUpdate = settingsViewModel::update,
            onLanguage = settingsViewModel::setLanguage,
            onTestVoice = clockViewModel::onSpeak,
            onSetPin = { screen = Screen.SetPin },
            onRemovePin = { settingsViewModel.update { it.copy(helperPinHash = null) } },
            onNeedNotifications = askForNotifications,
            onBack = { screen = Screen.Clock },
        )
        Screen.SetPin -> SetPinScreen(
            onPinSet = { pin ->
                settingsViewModel.update { it.copy(helperPinHash = HelperPin.hash(pin)) }
                screen = Screen.Settings
            },
            onCancel = { screen = Screen.Settings },
        )
    }
}

/** Scales all text by the helper's choice, on top of the phone's own font size. */
@Composable
private fun TextSizeScale(size: TextSize, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * size.scale),
        content = content,
    )
}

@Composable
private fun ClockScreen(voice: VoiceStatus, onSpeak: () -> Unit, onOpenSettings: () -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Tick at the start of each second so the minute flips on time.
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }

    ClockContent(
        timeText = now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
        dayText = now.format(DateTimeFormatter.ofPattern("EEEE")),
        voice = voice,
        onSpeak = onSpeak,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun ClockContent(
    timeText: String,
    dayText: String,
    voice: VoiceStatus,
    onSpeak: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val speakLabel = stringResource(R.string.speak_time)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Tapping anywhere on the time speaks it. TalkBack reads time and day as one item.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { heading() }
                .clickable(onClickLabel = speakLabel, onClick = onSpeak),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = dayText,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        // Always shown, in the same place, so nothing on the screen moves when the voice changes.
        // TalkBack reads it out when it changes.
        Text(
            text = stringResource(voiceMessage(voice)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        BigButton(text = speakLabel, onClick = onSpeak)
        SecondaryButton(
            text = stringResource(R.string.settings_for_helpers),
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClockContentPreview() {
    NikoTheme {
        ClockContent(
            timeText = "10:30 AM",
            dayText = "Saturday",
            voice = VoiceStatus.Ready,
            onSpeak = {},
            onOpenSettings = {},
        )
    }
}
