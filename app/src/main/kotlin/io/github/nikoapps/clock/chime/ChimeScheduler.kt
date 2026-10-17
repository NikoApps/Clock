package io.github.nikoapps.clock.chime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import io.github.nikoapps.clock.settings.Settings
import java.time.ZonedDateTime

/** Schedules the next chime as an exact alarm, one at a time. */
object ChimeScheduler {

    /** Registered with [io.github.nikoapps.clock.settings.BackgroundSync]. */
    fun sync(context: Context, settings: Settings) {
        if (settings.chimeEnabled) scheduleNext(context, settings, ZonedDateTime.now()) else cancel(context)
    }

    fun scheduleNext(context: Context, settings: Settings, after: ZonedDateTime) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        val at = nextChimeAfter(after, settings.chimeIntervalMinutes).toInstant().toEpochMilli()
        val operation = pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT, at) ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        } else {
            // Only on Android 12 if a helper took the permission away: late is better than silent.
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

    /** [at] travels with the alarm, so a chime that fires a moment early doesn't schedule itself again. */
    private fun pendingIntent(context: Context, flags: Int, at: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ChimeReceiver::class.java)
                .setAction(ChimeReceiver.ACTION_CHIME)
                .putExtra(ChimeReceiver.EXTRA_AT, at),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
}
