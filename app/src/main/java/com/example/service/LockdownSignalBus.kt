package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process lockdown event bus allowing Quick Settings tiles, the Home Screen Vault Widget,
 * and background services to immediately lock any active in-app PureLock session.
 */
object LockdownSignalBus {
    private val _lockdownEvents = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    val lockdownEvents: SharedFlow<Long> = _lockdownEvents.asSharedFlow()

    fun emitLockdown() {
        _lockdownEvents.tryEmit(System.currentTimeMillis())
    }
}
