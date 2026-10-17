package io.github.nikoapps.clock.chime

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.getSystemService
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.clock.settings.SettingsRepository
import io.github.nikoapps.clock.speech.Announcement
import io.github.nikoapps.clock.speech.SpeakService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Fires each chime and schedules the next one. Also puts the chime back after a restart,
 * an app update, a clock or time zone change, and when the exact alarm permission changes.
 *
 * Doesn't speak itself: Android may stop audio from a receiver, so it starts [SpeakService].
 */
class ChimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = SettingsRepository(appContext).current()
                if (intent.action == ACTION_CHIME) chime(appContext, intent, settings) else ChimeScheduler.sync(appContext, settings)
            } finally {
                pending.finish()
            }
        }
    }

    private fun chime(context: Context, intent: Intent, settings: Settings) {
        if (!settings.chimeEnabled) {
            ChimeScheduler.cancel(context)
            return
        }
        // Schedule from the planned time if the alarm fired a moment early, so it doesn't repeat.
        val now = ZonedDateTime.now()
        val planned = intent.getLongExtra(EXTRA_AT, 0L)
            .takeIf { it > 0L }
            ?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
        ChimeScheduler.scheduleNext(context, settings, if (planned != null && planned > now) planned else now)

        if (settings.isQuiet(LocalTime.now())) return
        if (settings.chimeFollowsDoNotDisturb && isDoNotDisturbOn(context)) return
        try {
            SpeakService.start(context, Announcement.Chime)
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException: shouldn't happen from an exact alarm.
            Log.w(TAG, "Couldn't start the chime", e)
        }
    }

    private fun isDoNotDisturbOn(context: Context): Boolean {
        val filter = context.getSystemService<NotificationManager>()?.currentInterruptionFilter
        return filter != null &&
            filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    companion object {
        const val ACTION_CHIME = "io.github.nikoapps.clock.action.CHIME"
        const val EXTRA_AT = "at"
        private const val TAG = "ChimeReceiver"
    }
}
