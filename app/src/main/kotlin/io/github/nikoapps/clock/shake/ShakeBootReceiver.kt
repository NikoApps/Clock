package io.github.nikoapps.clock.shake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.nikoapps.clock.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Starts shake listening again after the phone restarts or Niko Clock is updated.
 * Android allows a `specialUse` foreground service to start from these broadcasts.
 */
class ShakeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ShakeService.sync(context, SettingsRepository(context).current())
            } finally {
                pending.finish()
            }
        }
    }
}
