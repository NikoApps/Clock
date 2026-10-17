package io.github.nikoapps.clock.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Reads and writes [Settings]. One DataStore per process, shared by the app, services and receivers. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<Settings> = store.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        store.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs.write(updated)
        }
    }

    private object Keys {
        val language = stringPreferencesKey("language")
        val speechRate = floatPreferencesKey("speech_rate")
        val chimeEnabled = booleanPreferencesKey("chime_enabled")
        val chimeInterval = intPreferencesKey("chime_interval_minutes")
        val chimeVolume = intPreferencesKey("chime_volume_percent")
        val quietEnabled = booleanPreferencesKey("quiet_enabled")
        val quietStart = intPreferencesKey("quiet_start_minute")
        val quietEnd = intPreferencesKey("quiet_end_minute")
        val chimeFollowsDnd = booleanPreferencesKey("chime_follows_dnd")
        val shakeEnabled = booleanPreferencesKey("shake_enabled")
        val shakeSensitivity = stringPreferencesKey("shake_sensitivity")
        val vibrate = booleanPreferencesKey("vibrate_before_speaking")
        val pinHash = stringPreferencesKey("helper_pin_hash")
    }

    private fun Preferences.toSettings(): Settings {
        val d = Settings()
        return Settings(
            language = this[Keys.language],
            speechRate = this[Keys.speechRate] ?: d.speechRate,
            chimeEnabled = this[Keys.chimeEnabled] ?: d.chimeEnabled,
            chimeIntervalMinutes = this[Keys.chimeInterval] ?: d.chimeIntervalMinutes,
            chimeVolumePercent = this[Keys.chimeVolume] ?: d.chimeVolumePercent,
            quietHoursEnabled = this[Keys.quietEnabled] ?: d.quietHoursEnabled,
            quietStart = this[Keys.quietStart]?.let(::minuteToTime) ?: d.quietStart,
            quietEnd = this[Keys.quietEnd]?.let(::minuteToTime) ?: d.quietEnd,
            chimeFollowsDoNotDisturb = this[Keys.chimeFollowsDnd] ?: d.chimeFollowsDoNotDisturb,
            shakeEnabled = this[Keys.shakeEnabled] ?: d.shakeEnabled,
            shakeSensitivity = this[Keys.shakeSensitivity]
                ?.let { name -> ShakeSensitivity.entries.firstOrNull { it.name == name } }
                ?: d.shakeSensitivity,
            vibrateBeforeSpeaking = this[Keys.vibrate] ?: d.vibrateBeforeSpeaking,
            helperPinHash = this[Keys.pinHash],
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(s: Settings) {
        if (s.language == null) remove(Keys.language) else this[Keys.language] = s.language
        this[Keys.speechRate] = s.speechRate
        this[Keys.chimeEnabled] = s.chimeEnabled
        this[Keys.chimeInterval] = s.chimeIntervalMinutes
        this[Keys.chimeVolume] = s.chimeVolumePercent
        this[Keys.quietEnabled] = s.quietHoursEnabled
        this[Keys.quietStart] = s.quietStart.toSecondOfDay() / 60
        this[Keys.quietEnd] = s.quietEnd.toSecondOfDay() / 60
        this[Keys.chimeFollowsDnd] = s.chimeFollowsDoNotDisturb
        this[Keys.shakeEnabled] = s.shakeEnabled
        this[Keys.shakeSensitivity] = s.shakeSensitivity.name
        this[Keys.vibrate] = s.vibrateBeforeSpeaking
        if (s.helperPinHash == null) remove(Keys.pinHash) else this[Keys.pinHash] = s.helperPinHash
    }

    private fun minuteToTime(minute: Int): LocalTime = LocalTime.of(minute / 60, minute % 60)
}
