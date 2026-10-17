package io.github.nikoapps.clock

import android.app.Application
import io.github.nikoapps.clock.chime.ChimeScheduler
import io.github.nikoapps.clock.settings.BackgroundSync
import io.github.nikoapps.clock.shake.ShakeService

class ClockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Speak on shake: starts or stops the listening service. The service follows
        // sensitivity, language and speed changes on its own while it runs.
        BackgroundSync.register(ShakeService::sync)
        // Chimes: schedules or cancels the next exact alarm.
        BackgroundSync.register(ChimeScheduler::sync)
    }
}
