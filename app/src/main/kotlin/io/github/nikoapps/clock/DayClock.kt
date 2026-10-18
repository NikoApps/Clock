package io.github.nikoapps.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
 */
@Composable
internal fun DayClockScreen(
    now: LocalDateTime,
    @StringRes hint: Int,
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

    KeepScreenOnWhileCharging()

    // A dark screen at night, whatever the phone's theme, so it doesn't light up the bedroom.
    NikoTheme(darkTheme = part == PartOfDay.Night || isSystemInDarkTheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ClockContent(hint = hint, onSpeak = onSpeak, onSpeakDate = onSpeakDate, onOpenSettings = onOpenSettings) {
                Icon(
                    painter = painterResource(part.icon()),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(88.dp),
                )
                BasicText(
                    text = now.format(dayFormat),
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 32.sp, maxFontSize = 80.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
                BasicText(
                    text = stringResource(part.label()),
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 64.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = now.format(dateFormat),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = now.format(timeFormat),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Keeps the screen on while the phone is plugged in, so the day clock can stay on a charger all day. */
@Composable
private fun KeepScreenOnWhileCharging() {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(context, view) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                view.keepScreenOn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
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
        onDispose {
            context.unregisterReceiver(receiver)
            view.keepScreenOn = false
        }
    }
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
