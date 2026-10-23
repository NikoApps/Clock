package io.github.nikoapps.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.github.nikoapps.commons.theme.NikoTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The day clock: the day of the week, the part of day and the date in large words, for people
 * who lose track of the day. Same frame as the normal clock, so tapping and the button work the
 * same. It turns dark at night, and the screen stays on while the phone is charging.
 * Settings and the PIN screen are never shown over the lock screen: the gear asks to unlock first.
 */
@Composable
internal fun DayClockScreen(
    now: LocalDateTime,
    nepaliDate: String?,
    note: String?,
    showOnLockScreen: Boolean,
    hint: String,
    onSpeak: () -> Unit,
    onSpeakDate: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dayFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEE", locale) }
    val dateFormat = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMMy"), locale)
    }
    val timeFormat = remember(locale) {
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }
    val part = PartOfDay.of(now.hour)

    val charging = rememberCharging()
    val view = LocalView.current
    val activity = LocalActivity.current
    DisposableEffect(charging, showOnLockScreen) {
        // On a charger the day clock stays on, and in front of the lock screen if the helper chose that.
        view.keepScreenOn = charging
        // Android 8.0 only has the old window flag for this; it's left out there.
        val canShowWhenLocked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
        if (canShowWhenLocked) activity?.setShowWhenLocked(charging && showOnLockScreen)
        onDispose {
            view.keepScreenOn = false
            if (canShowWhenLocked) activity?.setShowWhenLocked(false)
        }
    }

    // A dark screen at night, whatever the phone's theme, so it doesn't light up the bedroom.
    val dark = part == PartOfDay.Night || isSystemInDarkTheme()
    // The status bar and navigation bar icons follow that, not the phone's theme, so they stay
    // readable on the dark night screen of a phone in light mode. Back to the phone's on the way out.
    DisposableEffect(activity, dark) {
        val window = activity as? ComponentActivity
        val style = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark }
        window?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose { window?.enableEdgeToEdge() }
    }
    NikoTheme(darkTheme = dark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val extras = nepaliDate != null || !note.isNullOrBlank()
            ClockContent(
                hint = hint,
                onSpeak = onSpeak,
                onSpeakDate = onSpeakDate,
                onOpenSettings = onOpenSettings,
                compact = extras,
            ) {
                Icon(
                    painter = painterResource(part.icon()),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(if (extras) 56.dp else 88.dp),
                )
                // Every line shrinks to fit its share of the card when space runs out. The day and the
                // part of day get the biggest shares, so they stay the largest words at any text size.
                FitLine(
                    text = now.format(dayFormat),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxSize = 80.sp,
                    share = DAY_WORD_SHARE,
                )
                FitLine(
                    text = stringResource(part.label()),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxSize = 64.sp,
                    share = DAY_WORD_SHARE,
                )
                val lineStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal)
                FitLine(now.format(dateFormat), lineStyle, MaterialTheme.colorScheme.onSurface)
                FitLine(now.format(timeFormat), lineStyle, MaterialTheme.colorScheme.onSurfaceVariant)
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
    }
}

private const val DAY_WORD_SHARE = 2.5f

/** Whether the phone is plugged in, kept up to date. */
@Composable
private fun rememberCharging(): Boolean {
    val context = LocalContext.current
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            }
        }
        // The battery broadcast is sticky, so registering also returns the current state.
        val current = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (current != null) receiver.onReceive(context, current)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return charging
}

private fun PartOfDay.icon(): Int = when (this) {
    PartOfDay.Morning, PartOfDay.Evening -> R.drawable.ic_sun_horizon
    PartOfDay.Afternoon -> R.drawable.ic_sun
    PartOfDay.Night -> R.drawable.ic_moon
}

private fun PartOfDay.label(): Int = when (this) {
    PartOfDay.Morning -> R.string.part_morning
    PartOfDay.Afternoon -> R.string.part_afternoon
    PartOfDay.Evening -> R.string.part_evening
    PartOfDay.Night -> R.string.part_night
}
