package io.github.nikoapps.clock.settings

import android.content.Context
import android.os.UserManager
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File

private val stores = mutableMapOf<String, DataStore<Preferences>>()

/**
 * Clock's DataStore called [name], kept in device-protected storage so chimes, shake and the speak
 * service can read it before the first unlock after a restart (ADR 0036). One per process.
 *
 * Before 0.4.2 the files lived in normal storage, which stays locked until the first unlock. The
 * first open with the phone unlocked moves the old file over; an app update always runs unlocked.
 */
internal fun Context.deviceDataStore(name: String): DataStore<Preferences> = synchronized(stores) {
    stores.getOrPut(name) {
        val app = applicationContext
        val deviceDir = app.createDeviceProtectedStorageContext().filesDir
        // All of them at once, so a restart before one is first used can't leave it behind.
        // Stores already open in this process are left alone.
        if (app.getSystemService<UserManager>()?.isUserUnlocked == true) {
            (STORE_NAMES - stores.keys).forEach { moveStoreFile(storeFile(app.filesDir, it), storeFile(deviceDir, it)) }
        }
        PreferenceDataStoreFactory.create(produceFile = { storeFile(deviceDir, name) })
    }
}

/** Every DataStore Clock keeps. A new one must be added here so older installs move it too. */
private val STORE_NAMES = setOf("settings", "speech_log", "chime_slots")

private fun storeFile(filesDir: File, name: String) = File(filesDir, "datastore/$name.preferences_pb")

/**
 * Moves an old store file into place. The old file wins if both exist: it holds the helper's real
 * settings, and the new one can only hold defaults written before the move.
 */
internal fun moveStoreFile(from: File, to: File) {
    if (!from.exists()) return
    to.parentFile?.mkdirs()
    from.copyTo(to, overwrite = true)
    from.delete()
}
