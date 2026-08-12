package com.example

import android.app.Application
import com.example.util.FirebaseManager

class PureLockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseManager.initialize(this)
    }
}
