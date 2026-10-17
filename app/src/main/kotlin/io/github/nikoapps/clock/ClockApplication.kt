package io.github.nikoapps.clock

import android.app.Application
import io.github.nikoapps.clock.chime.ChimeScheduler
import io.github.nikoapps.clock.settings.BackgroundSync

class ClockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Features register their background sync here.
        BackgroundSync.register { _, _ -> }
        BackgroundSync.register(ChimeScheduler::sync)
    }
}
