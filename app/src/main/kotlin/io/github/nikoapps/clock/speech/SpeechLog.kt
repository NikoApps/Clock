package io.github.nikoapps.clock.speech

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.speechLogStore: DataStore<Preferences> by preferencesDataStore(name = "speech_log")

/**
 * When Niko Clock last spoke, so settings can show the last announcement and a
 * family member can check chimes are still working.
 */
class SpeechLog(context: Context) {
    private val store = context.applicationContext.speechLogStore

    /** Last time anything was spoken by the speak service: chime, tile or widget. */
    val lastSpokeAt: Flow<Instant?> = store.data.map { prefs -> prefs[Keys.lastSpoke]?.let(Instant::ofEpochMilli) }

    /** Last time a chime was spoken. */
    val lastChimeAt: Flow<Instant?> = store.data.map { prefs -> prefs[Keys.lastChime]?.let(Instant::ofEpochMilli) }

    suspend fun record(at: Instant, chime: Boolean) {
        store.edit { prefs ->
            prefs[Keys.lastSpoke] = at.toEpochMilli()
            if (chime) prefs[Keys.lastChime] = at.toEpochMilli()
        }
    }

    private object Keys {
        val lastSpoke = longPreferencesKey("last_spoke_at_millis")
        val lastChime = longPreferencesKey("last_chime_at_millis")
    }
}
