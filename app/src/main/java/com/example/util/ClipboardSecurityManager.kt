package com.example.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.R

/**
 * ClipboardSecurityManager ensures passwords, 2FA tokens, and secret notes
 * copied from the encrypted vault never leak or persist in the system clipboard.
 *
 * Implements:
 * - EXTRA_IS_SENSITIVE flag (Android 13+ / API 33) to suppress insecure clipboard overlays
 * - Configurable automatic clipboard wiping timer (10s - 120s, or 0 for manual clear)
 * - Live countdown state for UI notification badges
 * - Instant manual clipboard wipe (thread-safe across background services)
 */
object ClipboardSecurityManager {

    private val _clipboardClearCountdown = MutableStateFlow(0)
    val clipboardClearCountdown: StateFlow<Int> = _clipboardClearCountdown.asStateFlow()

    private val _isSensitiveClipActive = MutableStateFlow(false)
    val isSensitiveClipActive: StateFlow<Boolean> = _isSensitiveClipActive.asStateFlow()

    private val _lastCopiedLabel = MutableStateFlow<String?>(null)
    val lastCopiedLabel: StateFlow<String?> = _lastCopiedLabel.asStateFlow()

    private var autoClearJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * Copies a sensitive secret to the system clipboard with security flags
     * and starts the auto-clear timer if autoClearDurationSec > 0.
     */
    fun copySensitive(
        context: Context,
        label: String,
        sensitiveText: String,
        autoClearDurationSec: Int = 30
    ) {
        val appContext = context.applicationContext
        runOnMainThread {
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return@runOnMainThread

            val clip = ClipData.newPlainText(label, sensitiveText)

            // Android 13+ (API 33) flag to mark content as sensitive
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
                clip.description.extras = extras
            }

            try {
                clipboard.setPrimaryClip(clip)
            } catch (e: Exception) {
                return@runOnMainThread
            }
            _lastCopiedLabel.value = label
            _isSensitiveClipActive.value = true

            if (autoClearDurationSec > 0) {
                val toastMsg = if (label.isNotBlank()) {
                    appContext.getString(R.string.copied_item_clipboard_countdown, label, autoClearDurationSec)
                } else {
                    appContext.getString(R.string.copied_clipboard_countdown, autoClearDurationSec)
                }
                Toast.makeText(appContext, toastMsg, Toast.LENGTH_SHORT).show()
                startAutoClearTimer(appContext, autoClearDurationSec)
            } else {
                autoClearJob?.cancel()
                _clipboardClearCountdown.value = 0
                val toastMsg = if (label.isNotBlank()) {
                    appContext.getString(R.string.copied_item_toast, label)
                } else {
                    appContext.getString(R.string.btn_copy)
                }
                Toast.makeText(appContext, toastMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Starts or resets the auto-clear countdown timer.
     */
    private fun startAutoClearTimer(context: Context, durationSec: Int) {
        autoClearJob?.cancel()
        if (durationSec <= 0) {
            _clipboardClearCountdown.value = 0
            return
        }
        _clipboardClearCountdown.value = durationSec

        autoClearJob = scope.launch {
            for (sec in durationSec downTo 1) {
                _clipboardClearCountdown.value = sec
                delay(1000L)
            }
            _clipboardClearCountdown.value = 0
            clearClipboard(context, showNotification = true)
        }
    }

    /**
     * Wipes the clipboard content immediately (safe to call from any thread).
     */
    fun clearClipboard(context: Context, showNotification: Boolean = false) {
        val appContext = context.applicationContext
        autoClearJob?.cancel()
        _clipboardClearCountdown.value = 0
        _isSensitiveClipActive.value = false
        _lastCopiedLabel.value = null

        runOnMainThread {
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboard.clearPrimaryClip()
                    } else {
                        val emptyClip = ClipData.newPlainText("", "")
                        clipboard.setPrimaryClip(emptyClip)
                    }
                    if (showNotification) {
                        Toast.makeText(
                            appContext,
                            appContext.getString(R.string.clipboard_cleared_auto_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Defensive catch for OEM clipboard service anomalies
                }
            }
        }
    }
}
