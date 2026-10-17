package io.github.nikoapps.clock.speech

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.nikoapps.clock.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Short-lived foreground service that says the time once, then stops.
 *
 * Android 17 only lets background apps play audio from a foreground service, and chimes fire
 * with the app closed, so all speech that doesn't start from the open app goes through here.
 * Type `specialUse`: none of the other types fit a one-off spoken announcement, and
 * `shortService` doesn't count for background audio on Android 17.
 */
class SpeakService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var announcer: TimeAnnouncer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        // The alarm's own wake window is about 10 seconds; a slow engine can take longer.
        if (wakeLock == null) {
            wakeLock = getSystemService<PowerManager>()
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NikoClock:speak")
                ?.apply { acquire(WAKE_LOCK_TIMEOUT_MS) }
        }

        val kind = intent?.getStringExtra(EXTRA_KIND)
            ?.let { name -> Announcement.entries.firstOrNull { it.name == name } }
            ?: Announcement.Request
        val announcer = announcer ?: TimeAnnouncer(this).also { announcer = it }
        scope.launch {
            try {
                announcer.announce(kind)
            } finally {
                // Only stops if no newer request came in meanwhile.
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        createChannel(this)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_clock)
            .setContentTitle(getString(R.string.notification_speaking))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            // Before Android 14, 0 means "the type in the manifest".
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
    }

    override fun onDestroy() {
        scope.cancel()
        announcer?.shutdown()
        announcer = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_KIND = "kind"
        private const val CHANNEL_ID = "speaking"
        private const val NOTIFICATION_ID = 1
        private const val WAKE_LOCK_TIMEOUT_MS = 60_000L

        fun intent(context: Context, kind: Announcement): Intent =
            Intent(context, SpeakService::class.java).putExtra(EXTRA_KIND, kind.name)

        /** Starts the service. Throws if Android doesn't allow it right now. */
        fun start(context: Context, kind: Announcement) {
            ContextCompat.startForegroundService(context, intent(context, kind))
        }

        /** For widgets: a tap on a widget is allowed to start a foreground service. */
        fun pendingIntent(context: Context, kind: Announcement): PendingIntent =
            PendingIntent.getForegroundService(
                context,
                kind.ordinal,
                intent(context, kind),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun createChannel(context: Context) {
            val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.notification_channel_speaking))
                .setDescription(context.getString(R.string.notification_channel_speaking_description))
                .setShowBadge(false)
                .build()
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}
