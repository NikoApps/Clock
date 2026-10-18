package io.github.nikoapps.clock.chime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import io.github.nikoapps.clock.settings.Settings
import java.time.LocalTime
import java.time.ZonedDateTime

/** Greeting times a helper can pick. */
val GREETING_TIMES: List<LocalTime> = listOf(LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0))

/** The next [time] strictly after [now]: today if it's still ahead, otherwise tomorrow. */
fun nextGreetingAfter(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
    val today = now.with(time).withSecond(0).withNano(0)
    return if (today.isAfter(now)) today else today.plusDays(1).with(time)
}

/** Schedules the morning greeting as an exact alarm, the same way as chimes. */
object GreetingScheduler {

    /** Registered with [io.github.nikoapps.clock.settings.BackgroundSync]. */
    fun sync(context: Context, settings: Settings) {
        if (settings.greetingEnabled) scheduleNext(context, settings, ZonedDateTime.now()) else cancel(context)
    }

    fun scheduleNext(context: Context, settings: Settings, after: ZonedDateTime) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        val at = nextGreetingAfter(after, settings.greetingTime).toInstant().toEpochMilli()
        val operation = pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT, at) ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        pendingIntent(context, PendingIntent.FLAG_NO_CREATE, 0L)?.let {
            alarms.cancel(it)
            it.cancel()
        }
    }

    // Request code 1 keeps it apart from the chime alarm (0).
    private fun pendingIntent(context: Context, flags: Int, at: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, ChimeReceiver::class.java)
                .setAction(ChimeReceiver.ACTION_GREETING)
                .putExtra(ChimeReceiver.EXTRA_AT, at),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
}
