package io.github.nikoapps.clock.shake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.nikoapps.clock.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Starts shake listening again after the phone restarts or Niko Clock is updated. After a restart
 * it starts before the first unlock, so a locked phone answers a shake (ADR 0036).
 * Android allows a `specialUse` foreground service to start from these broadcasts.
 */
class ShakeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in STARTS) return
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

private val STARTS = setOf(
    Intent.ACTION_LOCKED_BOOT_COMPLETED,
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
)
