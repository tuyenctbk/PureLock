package com.example

import com.example.service.PasswordGeneratorConfig
import com.example.service.PasswordGeneratorService
import com.example.service.PasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PureLockSecurityLogicTest {

    private val service = PasswordGeneratorService()

    @Test
    fun testGeneratePassword_matchesLength() {
        val config = PasswordGeneratorConfig(length = 24)
        val password = service.generatePassword(config)
        assertEquals(24, password.length)
    }

    @Test
    fun testGeneratePassword_noAmbiguous() {
        val config = PasswordGeneratorConfig(length = 32, excludeAmbiguous = true)
        val password = service.generatePassword(config)
        val ambiguousChars = listOf('0', 'O', 'o', '1', 'l', 'I', '|')
        for (char in ambiguousChars) {
            assertTrue("Password should not contain ambiguous character $char", !password.contains(char))
        }
    }

    @Test
    fun testCalculateStrength_strongPassword() {
        val strongPass = "aB3!dE5@gH7#jK9$"
        val strength = service.calculateStrength(strongPass)
        assertTrue(strength == PasswordStrength.STRONG || strength == PasswordStrength.VERY_STRONG)
    }

    @Test
    fun testCalculateStrength_weakPassword() {
        val weakPass = "123456"
        val strength = service.calculateStrength(weakPass)
        assertEquals(PasswordStrength.WEAK, strength)
    }

    @Test
    fun testGeneratePin_correctLength() {
        val pin = service.generatePin(6)
        assertEquals(6, pin.length)
        assertTrue(pin.all { it.isDigit() })
    }
}
