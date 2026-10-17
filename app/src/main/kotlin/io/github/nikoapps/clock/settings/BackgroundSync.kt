package io.github.nikoapps.clock.settings

import android.content.Context

/**
 * Brings background work in line with [Settings] after they change, after boot and after an
 * app update. Each feature owns its own sync: chimes schedule or cancel exact alarms, shake
 * starts or stops its foreground service.
 */
object BackgroundSync {
    private val syncers = mutableListOf<(Context, Settings) -> Unit>()

    /** Features register once, from [io.github.nikoapps.clock.ClockApplication]. */
    fun register(syncer: (Context, Settings) -> Unit) {
        syncers += syncer
    }

    fun sync(context: Context, settings: Settings) {
        syncers.forEach { it(context.applicationContext, settings) }
    }
}
