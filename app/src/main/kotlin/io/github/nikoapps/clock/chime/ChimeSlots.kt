package io.github.nikoapps.clock.chime

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.chimeSlotStore: DataStore<Preferences> by preferencesDataStore(name = "chime_slots")

/** Which chime last fired, by its planned time, so one chime never sounds twice. */
class ChimeSlots(context: Context) {
    private val store = context.applicationContext.chimeSlotStore

    /** Marks the chime planned for [plannedAt] as fired. False if it already had. */
    suspend fun claim(plannedAt: Long): Boolean {
        var claimed = false
        store.edit { prefs ->
            claimed = prefs[lastFired] != plannedAt
            prefs[lastFired] = plannedAt
        }
        return claimed
    }

    private companion object {
        val lastFired = longPreferencesKey("last_fired_at_millis")
    }
}
