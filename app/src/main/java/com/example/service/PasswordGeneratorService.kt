package com.example.service

import java.security.SecureRandom

data class PasswordGeneratorConfig(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true
)

enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG,
    VERY_STRONG
}

class PasswordGeneratorService {

    private val random = SecureRandom()

    companion object {
        private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
        private const val DIGITS = "0123456789"
        private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    }

    fun generatePassword(config: PasswordGeneratorConfig): String {
        val charPool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        if (config.includeUppercase) {
            charPool.append(UPPER)
            guaranteedChars.add(UPPER[random.nextInt(UPPER.length)])
        }
        if (config.includeLowercase) {
            charPool.append(LOWER)
            guaranteedChars.add(LOWER[random.nextInt(LOWER.length)])
        }
        if (config.includeNumbers) {
            charPool.append(DIGITS)
            guaranteedChars.add(DIGITS[random.nextInt(DIGITS.length)])
        }
        if (config.includeSymbols) {
            charPool.append(SYMBOLS)
            guaranteedChars.add(SYMBOLS[random.nextInt(SYMBOLS.length)])
        }

        if (charPool.isEmpty()) {
            charPool.append(LOWER) // Fallback
        }

        val passwordChars = mutableListOf<Char>()
        passwordChars.addAll(guaranteedChars)

        val remainingLength = (config.length - guaranteedChars.size).coerceAtLeast(0)
        val poolStr = charPool.toString()
        for (i in 0 until remainingLength) {
            passwordChars.add(poolStr[random.nextInt(poolStr.length)])
        }

        passwordChars.shuffle(random)
        return passwordChars.joinToString("")
    }

    fun calculateEntropyBits(password: String): Double {
        if (password.isEmpty()) return 0.0
        var poolSize = 0
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32
        if (poolSize == 0) poolSize = 26

        return password.length * (kotlin.math.ln(poolSize.toDouble()) / kotlin.math.ln(2.0))
    }

    fun calculateStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.WEAK
        var score = 0
        if (password.length >= 10) score += 2
        if (password.length >= 16) score += 2
        if (password.any { it.isUpperCase() }) score += 1
        if (password.any { it.isLowerCase() }) score += 1
        if (password.any { it.isDigit() }) score += 1
        if (password.any { !it.isLetterOrDigit() }) score += 2

        return when {
            score >= 7 -> PasswordStrength.VERY_STRONG
            score >= 5 -> PasswordStrength.STRONG
            score >= 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}
