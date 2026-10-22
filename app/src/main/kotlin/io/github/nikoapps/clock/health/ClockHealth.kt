package io.github.nikoapps.clock.health

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import io.github.nikoapps.clock.R
import io.github.nikoapps.commons.health.HealthCheck
import io.github.nikoapps.commons.health.PhoneHealth
import io.github.nikoapps.clock.settings.Settings as ClockSettings

/** Checks the settings that silence talking clocks. The shared checks and their fixes live in Commons. */
object ClockHealth {

    /** Android 12 only: the helper took away the exact alarm permission, so chimes come late. */
    val ExactAlarms = HealthCheck(
        title = R.string.health_exact_alarms,
        fix = R.string.health_exact_alarms_value,
        isProblem = { context ->
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == false
        },
        fixIntent = { context -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, PhoneHealth.packageUri(context)) },
    )

    /** Problems that matter for what's turned on. Nothing runs in the background without chimes, the greeting or shake. */
    fun problems(context: Context, settings: ClockSettings): List<HealthCheck> {
        val alarms = settings.chimeEnabled || settings.greetingEnabled
        if (!alarms && !settings.shakeEnabled) return emptyList()
        val checks = buildList {
            if (alarms) add(ExactAlarms)
            add(PhoneHealth.Battery)
            add(PhoneHealth.Hibernation)
        }
        return PhoneHealth.problems(context, checks)
    }
}
