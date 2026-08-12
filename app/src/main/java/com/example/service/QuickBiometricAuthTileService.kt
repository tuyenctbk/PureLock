package com.example.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.N)
class QuickBiometricAuthTileService : TileService() {

    override fun onClick() {
        super.onClick()
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

            repository.logSecurityEvent(
                "QUICK_BIOMETRIC_REAUTH",
                "Quick Biometric Re-Auth triggered from Quick Settings / Always-on-Display tile."
            )

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("shortcut_action", "VIEW_VAULT")
            }
            withContext(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        applicationContext,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "Vault Re-Auth"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = "Biometric Unlock"
        }
        tile.updateTile()
    }
}
