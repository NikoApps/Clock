package io.github.nikoapps.clock.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import io.github.nikoapps.clock.speech.Announcement
import io.github.nikoapps.clock.speech.SpeakService
import io.github.nikoapps.clock.speech.TimeAnnouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick settings tile that says the time. Works with the phone locked. */
class SpeakTimeTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var fallback: TimeAnnouncer? = null

    override fun onStartListening() {
        // A one-tap action, not an on/off switch, so keep it looking ready.
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        try {
            SpeakService.start(this, Announcement.Request)
        } catch (e: IllegalStateException) {
            // If Android doesn't let the tile start the service, speak from here while the panel is open.
            Log.w(TAG, "Speaking from the tile instead of the service", e)
            val announcer = fallback ?: TimeAnnouncer(this).also { fallback = it }
            scope.launch { announcer.announce(Announcement.Request) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        fallback?.shutdown()
        fallback = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "SpeakTimeTile"
    }
}
