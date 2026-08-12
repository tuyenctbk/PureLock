package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.N)
class QuickLockdownTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val db = PureLockDatabase.getDatabase(applicationContext)
            val prefs = PureLockPreferences(applicationContext)
            val repository = PureLockRepository(
                applicationContext,
                db.appLockDao(),
                db.intruderDao(),
                db.logDao(),
                db.scheduleRuleDao(),
                db.encryptedVaultDao(),
                prefs
            )

            // Trigger immediate mass lockdown
            repository.setAllAppsLockState(true)
            repository.logSecurityEvent("QUICK_LOCKDOWN_TILE", "Emergency Quick Lockdown executed from Quick Settings Tile.")

            withContext(Dispatchers.Main) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Lockdown ACTIVE"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "All Apps Secured"
                }
                tile.updateTile()
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "Quick Lockdown"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Tap to Lock All"
        }
        tile.updateTile()
    }
}
