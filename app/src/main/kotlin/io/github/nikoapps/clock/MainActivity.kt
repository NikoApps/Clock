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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nikoapps.clock.settings.ClockHelperPin
import io.github.nikoapps.clock.ui.SetupScreen
import io.github.nikoapps.clock.calendar.BikramSambat
import io.github.nikoapps.clock.calendar.display
import android.app.KeyguardManager
import androidx.activity.compose.LocalActivity
import androidx.core.content.getSystemService
import io.github.nikoapps.clock.health.ClockHealth
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.nikoapps.clock.ui.SettingsScreen
import io.github.nikoapps.clock.ui.SettingsViewModel
import io.github.nikoapps.commons.settings.TextSize
import io.github.nikoapps.commons.speech.VoiceStatus
import io.github.nikoapps.commons.theme.NikoTheme
import io.github.nikoapps.commons.ui.BigButton
import io.github.nikoapps.commons.ui.SetPinScreen
import io.github.nikoapps.commons.ui.UnlockPinScreen
import io.github.nikoapps.commons.ui.voiceMessage
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
    val activity = LocalActivity.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val problems = remember(current, lifecycle) { ClockHealth.problems(context, current) }

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
            dayClockOnLockScreen = current.dayClockOnLockScreen,
            bikramSambat = current.bikramSambat,
            note = current.helperNote,
            onSpeak = clockViewModel::onSpeak,
            onSpeakDate = clockViewModel::onSpeakDate,
            onOpenSettings = {
                val open = { screen = if (current.helperPinHash == null) Screen.Settings else Screen.Unlock }
                // Over the lock screen, settings wait until the phone is unlocked.
                val keyguard = context.getSystemService<KeyguardManager>()
                if (activity != null && keyguard?.isKeyguardLocked == true) {
                    keyguard.requestDismissKeyguard(
                        activity,
                        object : KeyguardManager.KeyguardDismissCallback() {
                            override fun onDismissSucceeded() = open()
                        },
                    )
                } else {
                    open()
                }
            },
        )
        Screen.Unlock -> UnlockPinScreen(
            helperPin = ClockHelperPin,
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
                settingsViewModel.update { it.copy(helperPinHash = ClockHelperPin.hash(pin)) }
                screen = pinReturn
            },
            onCancel = { screen = pinReturn },
        )
    }
}

/** Scales all text by the helper's choice, on top of the phone's own font size, up to 200%. */
@Composable
private fun TextSizeScale(size: TextSize, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, size.fontScaleOn(density.fontScale)),
        content = content,
    )
}

@Composable
private fun ClockScreen(
    voice: VoiceStatus,
    needsAttention: Boolean,
    dayClock: Boolean,
    dayClockOnLockScreen: Boolean,
    bikramSambat: Boolean,
    note: String?,
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

    val hint = voiceMessage(voice)
        ?: stringResource(if (needsAttention) R.string.health_hint else R.string.status_ready)

    val nepaliDate = if (bikramSambat) {
        BikramSambat.fromGregorian(now.toLocalDate())?.display(nepali = locale.language == "ne")
    } else {
        null
    }

    if (dayClock) {
        DayClockScreen(
            now = now,
            nepaliDate = nepaliDate,
            note = note,
            showOnLockScreen = dayClockOnLockScreen,
            hint = hint,
            onSpeak = onSpeak,
            onSpeakDate = onSpeakDate,
            onOpenSettings = onOpenSettings,
        )
        return
    }
    val extras = nepaliDate != null || !note.isNullOrBlank()
    ClockContent(
        hint = hint,
        onSpeak = onSpeak,
        onSpeakDate = onSpeakDate,
        onOpenSettings = onOpenSettings,
        compact = extras,
    ) {
        // Every line shrinks to fit its share of the card when space runs out. The time gets the
        // biggest share, so it stays the largest thing on the screen at any text size.
        FitLine(
            text = now.format(timeFormat),
            style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.secondary,
            maxSize = 96.sp,
            share = TIME_SHARE,
        )
        FitLine(
            text = now.format(dateFormat),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (nepaliDate != null) {
            FitLine(
                text = nepaliDate,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!note.isNullOrBlank()) {
            FitLine(
                text = note,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

private const val TIME_SHARE = 3f

/**
 * One centred line that shrinks to fit when the card runs out of room. [share] is its part of the
 * room left over, against 1 for an ordinary line. The line height is in em, so a shrunk word keeps
 * its descenders.
 */
@Composable
internal fun ColumnScope.FitLine(
    text: String,
    style: TextStyle,
    color: Color,
    maxSize: TextUnit = style.fontSize,
    share: Float = 1f,
) {
    BasicText(
        text = text,
        style = style.copy(color = color, textAlign = TextAlign.Center, lineHeight = 1.2.em),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = maxSize),
        modifier = Modifier.weight(share, fill = false).fillMaxWidth(),
    )
}

/**
 * The main screen's fixed frame: title bar, the card that speaks on tap, the hint line and the
 * big button. [card] fills the card: the time and date, or the day clock.
 *
 * The hint and the button are measured first and always get their full height; the card takes
 * what's left. With large text or a long warning, the card's text shrinks rather than the hint
 * being cut off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClockContent(
    hint: String,
    onSpeak: () -> Unit,
    onSpeakDate: () -> Unit,
    onOpenSettings: () -> Unit,
    compact: Boolean = false,
    card: @Composable ColumnScope.() -> Unit,
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
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .semantics(mergeDescendants = true) {
                        heading()
                        customActions = listOf(CustomAccessibilityAction(speakDateLabel) { onSpeakDate(); true })
                    }
                    .combinedClickable(onClickLabel = speakLabel, onClick = onSpeak, onDoubleClick = onSpeakDate),
            ) {
                // A compact card leaves more room for its lines when it has many of them.
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = if (compact) 16.dp else 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 12.dp),
                ) {
                    card()
                }
            }
            // Always in the same place under the clock: how to use it, or what a helper needs to fix.
            // TalkBack reads it out when it changes. At large font sizes it shrinks to three lines,
            // so it never takes the room the clock needs.
            val hintStyle = MaterialTheme.typography.bodyMedium
            BasicText(
                text = hint,
                style = hintStyle.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 1.3.em,
                ),
                minLines = 2,
                maxLines = 3,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = hintStyle.fontSize),
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
        ClockContent(hint = stringResource(R.string.status_ready), onSpeak = {}, onSpeakDate = {}, onOpenSettings = {}) {
            FitLine("21:34:40", MaterialTheme.typography.displayLarge, MaterialTheme.colorScheme.secondary, 96.sp, TIME_SHARE)
            FitLine("Saturday, 17 October", MaterialTheme.typography.headlineMedium, MaterialTheme.colorScheme.onSurface)
        }
    }
}
