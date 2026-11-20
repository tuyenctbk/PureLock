package com.example.service

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    NOT_ENROLLED,
    UNAVAILABLE
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) {
            return if (!current.isFinishing && !current.isDestroyed) current else null
        }
        current = current.baseContext
    }
    return null
}

/**
 * BiometricPromptManager wraps androidx.biometric to protect application entry,
 * secure credentials, and authenticate privileged security actions with zero-cloud local guarantees.
 */
class BiometricPromptManager(private val context: Context) {

    fun checkBiometricAvailability(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun isBiometricSupported(): Boolean {
        val status = checkBiometricAvailability()
        return status == BiometricStatus.AVAILABLE || status == BiometricStatus.NOT_ENROLLED
    }

    fun isBiometricAvailable(): Boolean {
        return checkBiometricAvailability() == BiometricStatus.AVAILABLE
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = context.getString(R.string.biometric_prompt_title),
        subtitle: String = context.getString(R.string.biometric_prompt_subtitle),
        description: String? = null,
        negativeButtonText: String = context.getString(R.string.biometric_prompt_negative_btn),
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK)

        if (!description.isNullOrEmpty()) {
            promptBuilder.setDescription(description)
        }

        val promptInfo = promptBuilder.build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onDismiss()
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError(context.getString(R.string.biometric_scan_not_recognized))
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(context.getString(R.string.biometric_launch_error, e.localizedMessage ?: ""))
        }
    }

    fun authenticate(
        context: Context,
        title: String = this.context.getString(R.string.biometric_prompt_title),
        subtitle: String = this.context.getString(R.string.biometric_prompt_subtitle),
        description: String? = null,
        negativeButtonText: String = this.context.getString(R.string.biometric_prompt_negative_btn),
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val activity = context.findFragmentActivity()
        if (activity != null) {
            when (checkBiometricAvailability()) {
                BiometricStatus.AVAILABLE -> {
                    showBiometricPrompt(
                        activity = activity,
                        title = title,
                        subtitle = subtitle,
                        description = description,
                        negativeButtonText = negativeButtonText,
                        onSuccess = onSuccess,
                        onError = onError,
                        onDismiss = onDismiss
                    )
                }
                BiometricStatus.NOT_ENROLLED -> {
                    onError(context.getString(R.string.biometric_not_enrolled_error))
                }
                BiometricStatus.NO_HARDWARE, BiometricStatus.UNAVAILABLE -> {
                    onError(context.getString(R.string.biometric_unavailable_error))
                }
            }
        } else {
            onError(context.getString(R.string.biometric_no_activity_error))
        }
    }
}
