package io.github.nikoapps.clock

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
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
import io.github.nikoapps.clock.ui.SetupScreen
import io.github.nikoapps.clock.health.PhoneHealth
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.nikoapps.clock.ui.SettingsScreen
import io.github.nikoapps.clock.ui.SettingsViewModel
import io.github.nikoapps.clock.ui.UnlockPinScreen
import io.github.nikoapps.clock.ui.voiceMessage
import io.github.nikoapps.commons.theme.NikoTheme
import io.github.nikoapps.commons.ui.BigButton
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

private enum class Screen { Clock, Unlock, Settings, SetPin, Setup }

@Composable
private fun App(settingsViewModel: SettingsViewModel, clockViewModel: ClockViewModel = viewModel()) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val clock by clockViewModel.state.collectAsStateWithLifecycle()
    val lastChimeAt by settingsViewModel.lastChimeAt.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Clock) }
    var setupStep by rememberSaveable { mutableIntStateOf(0) }
    // The PIN screen opens from settings or from setup, and goes back to where it came from.
    var pinReturn by rememberSaveable { mutableStateOf(Screen.Settings) }

    // Chimes and shake show a notification while they run; Android 13+ asks first.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val askForNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = screen != Screen.Clock) {
        screen = if (screen == Screen.SetPin) pinReturn else Screen.Clock
    }

    val current = settings ?: return

    // Checked again each time the app comes back, such as after fixing a setting in the phone's settings.
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val problems = remember(current, lifecycle) { PhoneHealth.problems(context, current) }

    // First run is for the helper. A phone that already has a PIN is past that, and skipping the
    // PIN for setup would let anyone into settings.
    LaunchedEffect(Unit) {
        if (!current.setupDone && current.helperPinHash == null && screen == Screen.Clock) screen = Screen.Setup
    }
    val finishSetup = {
        settingsViewModel.update { it.copy(setupDone = true) }
        setupStep = 0
        screen = Screen.Clock
    }

    when (screen) {
        Screen.Clock -> ClockScreen(
            voice = clock.voice,
            needsAttention = problems.isNotEmpty(),
            dayClock = current.dayClock,
            onSpeak = clockViewModel::onSpeak,
            onSpeakDate = clockViewModel::onSpeakDate,
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
            engine = clock.engine,
            problems = problems,
            lastChimeAt = lastChimeAt,
            screenLanguage = settingsViewModel.screenLanguage(),
            canChooseScreenLanguage = settingsViewModel.canChooseScreenLanguage,
            onUpdate = settingsViewModel::update,
            onScreenLanguage = settingsViewModel::setScreenLanguage,
            onTestVoice = clockViewModel::onSpeak,
            onSetPin = { pinReturn = Screen.Settings; screen = Screen.SetPin },
            onRemovePin = { settingsViewModel.update { it.copy(helperPinHash = null) } },
            onNeedNotifications = askForNotifications,
            onStartSetup = { setupStep = 0; screen = Screen.Setup },
            onBack = { screen = Screen.Clock },
        )
        Screen.Setup -> SetupScreen(
            step = setupStep,
            onStep = { setupStep = it },
            settings = current,
            voice = clock.voice,
            engine = clock.engine,
            problems = problems,
            onUpdate = settingsViewModel::update,
            onTestVoice = clockViewModel::onSpeak,
            onSetPin = { pinReturn = Screen.Setup; screen = Screen.SetPin },
            onRemovePin = { settingsViewModel.update { it.copy(helperPinHash = null) } },
            onNeedNotifications = askForNotifications,
            onFinish = finishSetup,
        )
        Screen.SetPin -> SetPinScreen(
            onPinSet = { pin ->
                settingsViewModel.update { it.copy(helperPinHash = HelperPin.hash(pin)) }
                screen = pinReturn
            },
            onCancel = { screen = pinReturn },
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
private fun ClockScreen(
    voice: VoiceStatus,
    needsAttention: Boolean,
    dayClock: Boolean,
    onSpeak: () -> Unit,
    onSpeakDate: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Tick at the start of each second so the seconds and the minute flip on time.
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }

    // Follow the phone's 12 or 24 hour choice and its way of writing dates.
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(locale) {
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hms" else "hms"
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }
    val dateFormat = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale)
    }

    val hint = when {
        voice != VoiceStatus.Ready && voice != VoiceStatus.Starting -> voiceMessage(voice)
        needsAttention -> R.string.health_hint
        else -> R.string.status_ready
    }

    if (dayClock) {
        DayClockScreen(now = now, hint = hint, onSpeak = onSpeak, onSpeakDate = onSpeakDate, onOpenSettings = onOpenSettings)
        return
    }
    ClockContent(hint = hint, onSpeak = onSpeak, onSpeakDate = onSpeakDate, onOpenSettings = onOpenSettings) {
        TimeAndDate(timeText = now.format(timeFormat), dateText = now.format(dateFormat))
    }
}

@Composable
private fun TimeAndDate(timeText: String, dateText: String) {
    // Shrinks to fit so the seconds never wrap, at any text size or in any language.
    BasicText(
        text = timeText,
        style = MaterialTheme.typography.displayLarge.copy(
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            fontFeatureSettings = "tnum",
        ),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 32.sp, maxFontSize = 96.sp),
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = dateText,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

/**
 * The main screen's fixed frame: title bar, the card that speaks on tap, the hint line and the
 * big button. [card] fills the card: the time and date, or the day clock.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClockContent(
    @StringRes hint: Int,
    onSpeak: () -> Unit,
    onSpeakDate: () -> Unit,
    onOpenSettings: () -> Unit,
    card: @Composable () -> Unit,
) {
    val speakLabel = stringResource(R.string.speak_time)
    val speakDateLabel = stringResource(R.string.speak_date)
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
            actions = {
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(64.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_open),
                        modifier = Modifier.size(32.dp),
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Tap the clock to hear the time, tap it twice to hear the date. TalkBack reads time and
            // date as one item and offers "Speak the date" as an action, since double taps are its own.
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .semantics(mergeDescendants = true) {
                        heading()
                        customActions = listOf(CustomAccessibilityAction(speakDateLabel) { onSpeakDate(); true })
                    }
                    .combinedClickable(onClickLabel = speakLabel, onClick = onSpeak, onDoubleClick = onSpeakDate),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    card()
                }
            }
            // Always in the same place under the clock: how to use it, or what a helper needs to fix.
            // TalkBack reads it out when it changes.
            Text(
                text = stringResource(hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        BigButton(
            text = speakLabel,
            onClick = onSpeak,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClockContentPreview() {
    NikoTheme {
        ClockContent(hint = R.string.status_ready, onSpeak = {}, onSpeakDate = {}, onOpenSettings = {}) {
            TimeAndDate(timeText = "21:34:40", dateText = "Saturday, 17 October")
        }
    }
}
