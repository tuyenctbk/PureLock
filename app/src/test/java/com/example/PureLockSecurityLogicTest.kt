package com.example

import com.example.data.PureLockRepository
import com.example.data.model.ScheduleRuleEntity
import com.example.service.PasswordGeneratorConfig
import com.example.service.PasswordGeneratorService
import com.example.service.PasswordStrength
import com.example.ui.evaluateCalculatorExpression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

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

    @Test
    fun testScheduleRule_overnightMatchesNextMorningNotSameMorning() {
        val rule = ScheduleRuleEntity(
            packageName = "com.example.chat",
            appName = "Chat",
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            isEnabled = true,
            daysString = "MON"
        )

        // Monday 23:00 -> active (started Mon 22:00)
        assertTrue(PureLockRepository.isScheduleRuleActiveAt(rule, Calendar.MONDAY, 23, 0))
        // Tuesday 02:30 -> active (rollover from Mon 22:00)
        assertTrue(PureLockRepository.isScheduleRuleActiveAt(rule, Calendar.TUESDAY, 2, 30))
        // Monday 02:30 -> INACTIVE (Sunday was not enabled)
        assertFalse(PureLockRepository.isScheduleRuleActiveAt(rule, Calendar.MONDAY, 2, 30))
        // Tuesday 23:00 -> INACTIVE (Tuesday evening is not enabled)
        assertFalse(PureLockRepository.isScheduleRuleActiveAt(rule, Calendar.TUESDAY, 23, 0))
        // Disabled rule -> INACTIVE
        assertFalse(PureLockRepository.isScheduleRuleActiveAt(rule.copy(isEnabled = false), Calendar.MONDAY, 23, 0))
    }

    @Test
    fun testScheduleRule_daytimeWindow() {
        val rule = ScheduleRuleEntity(
            packageName = "com.example.game",
            appName = "Game",
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 30,
            isEnabled = true,
            daysString = "MON,WED,FRI"
        )

        assertTrue(PureLockRepository.isScheduleRuleActiveAt(rule, Calendar.WEDNESDAY, 12, 0))
        assertFalse(PureLockRepository.isScheduleRuleActiveAt(rule, Calendar.WEDNESDAY, 18, 0))
        assertFalse(PureLockRepository.isScheduleRuleActiveAt(rule, Calendar.TUESDAY, 12, 0))
    }

    @Test
    fun testCalculatorExpression_handlesOpsPercentAndNegatives() {
        assertEquals("14", evaluateCalculatorExpression("8+6"))
        assertEquals("42", evaluateCalculatorExpression("6×7"))
        assertEquals("Error", evaluateCalculatorExpression("9÷0"))
        assertEquals("-3", evaluateCalculatorExpression("-5+2"))
        assertEquals("0.5", evaluateCalculatorExpression("50%"))
        assertEquals("1", evaluateCalculatorExpression("10%3"))
    }

    @Test
    fun testCalculatorExpression_consistentAcrossCommaDecimalLocales() {
        val prevLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.FRANCE)
            assertEquals("0.5", evaluateCalculatorExpression("50%"))
            assertEquals("2.50", evaluateCalculatorExpression("5÷2"))
        } finally {
            java.util.Locale.setDefault(prevLocale)
        }
    }
}
