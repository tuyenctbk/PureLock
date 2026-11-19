package com.example

import android.app.Application
import com.example.data.PureLockDatabase
import com.example.util.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PureLockApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: PureLockDatabase by lazy {
        PureLockDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try {
                PureLockDatabase.ensureSqlCipherLoaded()
                // Pre-warm Room/Keystore passphrase unwrap off the main thread
                PureLockDatabase.getDatabase(this@PureLockApplication)
            } catch (_: Throwable) {
                // Handled gracefully on subsequent repository access
            }
        }
        FirebaseManager.initialize(this)
    }
}
