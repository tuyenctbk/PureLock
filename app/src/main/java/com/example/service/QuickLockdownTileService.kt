package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.R
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.N)
class QuickLockdownTileService : TileService() {

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onClick() {
        super.onClick()
        val tile = qsTile

        tileScope.launch {
            try {
                val db = PureLockDatabase.getDatabase(applicationContext)
                val prefs = PureLockPreferences(applicationContext)
                val repository = PureLockRepository(
                    applicationContext,
                    db.appLockDao(),
                    db.intruderDao(),
                    db.logDao(),
                    db.scheduleRuleDao(),
                    db.encryptedVaultDao(),
                    db.userSettingDao(),
                    prefs
                )

                // Trigger immediate mass lockdown
                repository.setAllAppsLockState(true)
                repository.lockAllAndClearSessions()
                com.example.util.ClipboardSecurityManager.clearClipboard(applicationContext, showNotification = false)
                repository.logSecurityEvent("QUICK_LOCKDOWN_TILE", "Emergency Quick Lockdown executed from Quick Settings Tile.")
            } catch (_: Exception) {
            }

            withContext(Dispatchers.Main) {
                try {
                    val activeTile = qsTile ?: tile ?: return@withContext
                    activeTile.state = Tile.STATE_ACTIVE
                    activeTile.label = getString(R.string.tile_quick_lockdown_active)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        activeTile.subtitle = getString(R.string.tile_quick_lockdown_active_subtitle)
                    }
                    activeTile.updateTile()
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_quick_lockdown_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(R.string.tile_quick_lockdown_subtitle)
        }
        try {
            tile.updateTile()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        tileScope.cancel()
        super.onDestroy()
    }
}
