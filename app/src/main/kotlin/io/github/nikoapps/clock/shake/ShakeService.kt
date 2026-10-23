package io.github.nikoapps.clock.shake

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.nikoapps.clock.MainActivity
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.clock.settings.SettingsRepository
import io.github.nikoapps.clock.speech.TimePhraser
import io.github.nikoapps.clock.speech.spokenDate
import io.github.nikoapps.clock.speech.phraserFor
import io.github.nikoapps.clock.speech.awaitVoiceFor
import io.github.nikoapps.commons.speech.Speaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Listens for a shake, even with the phone locked and the screen off, and speaks the time.
 *
 * Android only sends accelerometer events to apps in the foreground, so this is a persistent
 * foreground service with a notification. It runs only while "Speak on shake" is on, and holds
 * a partial wake lock: the accelerometer doesn't wake the phone, so without it events stop
 * when the screen goes off. See Hub docs/06-engineering/05-background-reliability.md.
 */
class ShakeService : Service(), SensorEventListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val detector = ShakeDetector()

    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var speaker: Speaker

    private var settings = Settings()
    private var phraser: TimePhraser? = null
    private var listening = false

    /** When the last shake spoke the time, so a second shake soon after can speak the date. */
    private var lastTimeSpokenAt: Long? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Someone asked for the time, so speak on the accessibility stream: Do Not Disturb never mutes it.
        speaker = Speaker(this, AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        sensorManager = getSystemService(SensorManager::class.java)
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NikoClock:shake")
            .apply { setReferenceCounted(false) }

        scope.launch {
            SettingsRepository(this@ShakeService).settings.collect { latest ->
                // Turned off while the service was being restarted, or by a helper.
                if (!latest.shakeEnabled) {
                    stopSelf()
                    return@collect
                }
                settings = latest
                detector.sensitivity = latest.shakeSensitivity
                phraser = phraserFor(latest)
                speaker.setRate(latest.speechRate)
                speaker.setPitch(latest.voicePitch.pitch)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must come first, within seconds of the start, every time the service is started.
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                },
            )
        } catch (e: IllegalStateException) {
            // Android can refuse when it restarts the service from the background. Stop
            // instead of crashing; opening the app or a restart brings it back.
            Log.w(TAG, "Not allowed to listen in the foreground right now", e)
            stopSelf()
            return START_NOT_STICKY
        }
        startListening()
        // Android restarts the service if it kills the process.
        return START_STICKY
    }

    // Held for as long as shake listening is on, by design; released in onDestroy.
    @SuppressLint("WakelockTimeout")
    private fun startListening() {
        if (listening) return
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Log.w(TAG, "No accelerometer, shake can't work on this phone")
            stopSelf()
            return
        }
        wakeLock.acquire()
        detector.reset()
        // SENSOR_DELAY_GAME (about 50 a second) sees each back-and-forth of a quick shake.
        // No batching delay, so the time is spoken straight away.
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, 0)
        listening = true
    }

    override fun onSensorChanged(event: SensorEvent) {
        val v = event.values
        if (detector.onSample(v[0], v[1], v[2], event.timestamp)) onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Shake once for the time. Shake again within a few seconds for the date. */
    private fun onShake() {
        val now = SystemClock.elapsedRealtime()
        val last = lastTimeSpokenAt
        if (settings.shakeAgainForDate && last != null && now - last < DATE_WINDOW_MS) {
            lastTimeSpokenAt = null
            speak { spokenDate(it, settings, LocalDate.now()) }
        } else {
            lastTimeSpokenAt = now
            speak { it.phrase(LocalTime.now()) }
        }
    }

    private fun speak(sentence: (TimePhraser) -> String) {
        val phraser = phraser ?: return
        scope.launch {
            // Never fall back to a voice in another language.
            if (!speaker.awaitVoiceFor(phraser.locale)) {
                Log.w(TAG, "No offline voice for ${phraser.locale}, staying quiet")
                return@launch
            }
            if (settings.vibrateBeforeSpeaking) {
                vibrate()
                delay(VIBRATION_MS + 150)
            }
            // Quiet hours don't apply: the person asked for the time.
            speaker.speak(sentence(phraser))
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY))
        } else {
            vibrator.vibrate(effect)
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        // Low importance: shown quietly, no sound or pop-up.
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.shake_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.shake_channel_description) },
        )
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = getString(R.string.shake_notification_text)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shake)
            .setContentTitle(getString(R.string.shake_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        if (listening) sensorManager.unregisterListener(this)
        listening = false
        if (wakeLock.isHeld) wakeLock.release()
        scope.cancel()
        speaker.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ShakeService"
        private const val CHANNEL_ID = "shake"
        private const val NOTIFICATION_ID = 2001
        private const val VIBRATION_MS = 150L

        // A shake can't fire again for 3 s, so this leaves about 7 s to shake again for the date.
        private const val DATE_WINDOW_MS = 10_000L

        /** Starts or stops shake listening to match [settings]. Registered with BackgroundSync. */
        fun sync(context: Context, settings: Settings) {
            val intent = Intent(context, ShakeService::class.java)
            if (settings.shakeEnabled) {
                try {
                    ContextCompat.startForegroundService(context, intent)
                } catch (e: IllegalStateException) {
                    // Android 12+ refuses to start it from the background. It starts again the
                    // next time the app is opened, after a restart or after an update.
                    Log.w(TAG, "Couldn't start shake listening from the background", e)
                }
            } else {
                context.stopService(intent)
            }
        }

        /**
         * True when Android 13+ needs the notification permission before the "listening"
         * notification can show. Shake still works without it, but the person can't see it's on.
         * Request [Manifest.permission.POST_NOTIFICATIONS] when shake is turned on.
         */
        fun needsNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
    }
}
