package io.github.nikoapps.clock

import android.app.Application
import io.github.nikoapps.clock.settings.BackgroundSync
import io.github.nikoapps.clock.shake.ShakeService

class ClockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Features register their background sync here.
        BackgroundSync.register { _, _ -> }
        // Speak on shake: starts or stops the listening service. The service follows
        // sensitivity, language and speed changes on its own while it runs.
        BackgroundSync.register(ShakeService::sync)
    }
}
