package io.github.nikoapps.clock.health

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import io.github.nikoapps.clock.settings.Settings as ClockSettings

/** A phone setting that can stop chimes or shake from working. */
enum class HealthProblem {
    /** Android 12 only: the helper took away the exact alarm permission, so chimes come late. */
    ExactAlarms,

    /** Battery optimisation is on, so the phone may stop Niko Clock in the background. */
    Battery,

    /** "Pause app activity if unused" is on. A person who never opens the app would lose chimes after a few months. */
    Hibernation,
}

/** Checks the settings that silence talking clocks, and opens the page that fixes each one. */
object PhoneHealth {

    /** Problems that matter for what's turned on. Nothing runs in the background without chimes, the greeting or shake. */
    fun problems(context: Context, settings: ClockSettings): List<HealthProblem> {
        val alarms = settings.chimeEnabled || settings.greetingEnabled
        if (!alarms && !settings.shakeEnabled) return emptyList()
        return buildList {
            if (alarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == false
            ) {
                add(HealthProblem.ExactAlarms)
            }
            if (context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) == false) {
                add(HealthProblem.Battery)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !context.packageManager.isAutoRevokeWhitelisted) {
                add(HealthProblem.Hibernation)
            }
        }
    }

    fun fixIntent(context: Context, problem: HealthProblem): Intent {
        val packageUri = "package:${context.packageName}".toUri()
        return when (problem) {
            HealthProblem.ExactAlarms -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)
            // App info has "Battery" or "App battery usage" one tap away on most phones. Asking
            // directly would need another permission.
            HealthProblem.Battery -> appInfo(context)
            HealthProblem.Hibernation -> Intent(Intent.ACTION_AUTO_REVOKE_PERMISSIONS, packageUri)
        }
    }

    fun appInfo(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
}

/** Phone makers with their own battery killers on top of Android's. */
enum class PhoneMaker {
    Xiaomi,
    Samsung,
    Other,
    ;

    /** The maker's own page where it has one, otherwise app info. Callers fall back to app info if it won't open. */
    fun settingsIntent(context: Context): Intent = when (this) {
        Xiaomi -> Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )
        Samsung, Other -> PhoneHealth.appInfo(context)
    }

    companion object {
        fun current(): PhoneMaker = when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "redmi", "poco" -> Xiaomi
            "samsung" -> Samsung
            else -> Other
        }
    }
}
