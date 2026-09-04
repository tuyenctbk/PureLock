package com.example.service

import java.security.SecureRandom

data class PasswordGeneratorConfig(
    val length: Int = 16,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeAmbiguous: Boolean = false
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
        const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val LOWER = "abcdefghijklmnopqrstuvwxyz"
        const val DIGITS = "0123456789"
        const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        
        // Ambiguous characters: 0, O, o, 1, l, I, |
        const val UPPER_NO_AMBIGUOUS = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        const val LOWER_NO_AMBIGUOUS = "abcdefghijkmnopqrstuvwxyz"
        const val DIGITS_NO_AMBIGUOUS = "23456789"
        const val SYMBOLS_NO_AMBIGUOUS = "!@#$%^&*()_+-=[]{};:,.<>?"

        fun generateSecurePassword(
            length: Int = 16,
            includeUppercase: Boolean = true,
            includeLowercase: Boolean = true,
            includeDigits: Boolean = true,
            includeSymbols: Boolean = true,
            excludeAmbiguous: Boolean = false
        ): String {
            val service = PasswordGeneratorService()
            return service.generatePassword(
                PasswordGeneratorConfig(
                    length = length,
                    includeUppercase = includeUppercase,
                    includeLowercase = includeLowercase,
                    includeNumbers = includeDigits,
                    includeSymbols = includeSymbols,
                    excludeAmbiguous = excludeAmbiguous
                )
            )
        }
    }

    fun generatePassword(config: PasswordGeneratorConfig): String {
        val upperPool = if (config.excludeAmbiguous) UPPER_NO_AMBIGUOUS else UPPER
        val lowerPool = if (config.excludeAmbiguous) LOWER_NO_AMBIGUOUS else LOWER
        val digitsPool = if (config.excludeAmbiguous) DIGITS_NO_AMBIGUOUS else DIGITS
        val symbolsPool = if (config.excludeAmbiguous) SYMBOLS_NO_AMBIGUOUS else SYMBOLS

        val charPool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        if (config.includeUppercase) {
            charPool.append(upperPool)
            guaranteedChars.add(upperPool[random.nextInt(upperPool.length)])
        }
        if (config.includeLowercase) {
            charPool.append(lowerPool)
            guaranteedChars.add(lowerPool[random.nextInt(lowerPool.length)])
        }
        if (config.includeNumbers) {
            charPool.append(digitsPool)
            guaranteedChars.add(digitsPool[random.nextInt(digitsPool.length)])
        }
        if (config.includeSymbols) {
            charPool.append(symbolsPool)
            guaranteedChars.add(symbolsPool[random.nextInt(symbolsPool.length)])
        }

        if (charPool.isEmpty()) {
            charPool.append(lowerPool)
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

    fun generatePin(digitsCount: Int = 4): String {
        val digits = StringBuilder()
        for (i in 0 until digitsCount) {
            digits.append(random.nextInt(10))
        }
        return digits.toString()
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

