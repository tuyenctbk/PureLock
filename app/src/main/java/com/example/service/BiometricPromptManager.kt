package com.example.service

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    NOT_ENROLLED,
    UNAVAILABLE
}

/**
 * BiometricPromptManager wraps androidx.biometric to protect application entry,
 * secure credentials, and authenticate privileged security actions.
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

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "PureLock Biometric Security",
        subtitle: String = "Verify your fingerprint or face to proceed",
        description: String? = "Unlock protected vault & system features",
        negativeButtonText: String = "Use PIN / Pattern",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
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
                    // If user cancelled, pass informative notice
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError("Biometric authentication cancelled.")
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Biometric scan not recognized. Try again.")
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError("Unable to launch biometric prompt: ${e.localizedMessage}")
        }
    }
}
