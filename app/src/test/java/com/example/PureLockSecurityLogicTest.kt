package com.example

import com.example.data.PureLockRepository
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.ScheduleRuleEntity
import com.example.service.PasswordGeneratorConfig
import com.example.service.PasswordGeneratorService
import com.example.service.PasswordStrength
import com.example.service.PureLockAutofillService
import com.example.ui.appendPatternNodeWithMidpoint
import com.example.ui.computeLockoutCooldownSeconds
import com.example.ui.evaluateCalculatorExpression
import com.example.ui.findCollinearMidpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun testPasswordGenerator_respectsShortTargetLength() {
        val shortPass = service.generatePassword(
            PasswordGeneratorConfig(
                length = 2,
                includeUppercase = true,
                includeLowercase = true,
                includeNumbers = true,
                includeSymbols = true
            )
        )
        assertEquals(2, shortPass.length)
    }

    @Test
    fun testBruteForceCooldownProgression() {
        assertEquals(0, computeLockoutCooldownSeconds(0))
        assertEquals(0, computeLockoutCooldownSeconds(4))
        assertEquals(15, computeLockoutCooldownSeconds(5))
        assertEquals(30, computeLockoutCooldownSeconds(8))
        assertEquals(60, computeLockoutCooldownSeconds(10))
    }

    @Test
    fun testPatternCollinearMidpoints() {
        assertEquals(2, findCollinearMidpoint(1, 3))
        assertEquals(4, findCollinearMidpoint(1, 7))
        assertEquals(5, findCollinearMidpoint(1, 9))
        assertEquals(5, findCollinearMidpoint(3, 7))
        assertEquals(6, findCollinearMidpoint(9, 3))
        assertNull(findCollinearMidpoint(1, 2))
        assertNull(findCollinearMidpoint(1, 6))
        assertNull(findCollinearMidpoint(4, 9))

        // Shared pattern builder used across Onboarding, Settings, and LockOverlay
        var nodes = listOf<Int>()
        nodes = appendPatternNodeWithMidpoint(nodes, 1)
        nodes = appendPatternNodeWithMidpoint(nodes, 3) // auto-inserts 2
        nodes = appendPatternNodeWithMidpoint(nodes, 9) // auto-inserts 6
        nodes = appendPatternNodeWithMidpoint(nodes, 7) // auto-inserts 8
        nodes = appendPatternNodeWithMidpoint(nodes, 3) // duplicate ignored
        assertEquals(listOf(1, 2, 3, 6, 9, 8, 7), nodes)
    }

    @Test
    fun testCalculatorExpression_zeroResultEvaluatesCleanly() {
        assertEquals("0", evaluateCalculatorExpression("5-5"))
        assertEquals("0", evaluateCalculatorExpression("0×9"))
    }

    @Test
    fun testAutofillCandidateSelection_restrictsToMatchesAndNeverLeaksNotes() {
        val vault = listOf(
            EncryptedVaultEntity(id = 1, title = "GitHub", secretContent = "gh_secret", category = "PASSWORD", websiteOrApp = "github.com"),
            EncryptedVaultEntity(id = 2, title = "Chase Bank", secretContent = "bank_secret", category = "PASSWORD", websiteOrApp = "chase.com"),
            EncryptedVaultEntity(id = 3, title = "Seed Phrase", secretContent = "twelve secret words", category = "NOTE", websiteOrApp = "")
        )

        // Direct web domain match returns ONLY matching credential (never unrelated bank or private note)
        val githubMatches = PureLockAutofillService.selectAutofillCandidates(
            vaultItems = vault,
            webDomains = setOf("github.com"),
            targetPackage = "com.android.chrome"
        )
        assertEquals(1, githubMatches.size)
        assertEquals("GitHub", githubMatches.first().title)

        // Unknown site fallback offers credential items only (strictly excludes NOTE items)
        val unknownMatches = PureLockAutofillService.selectAutofillCandidates(
            vaultItems = vault,
            webDomains = setOf("unknownsite.org"),
            targetPackage = "com.example.other"
        )
        assertEquals(2, unknownMatches.size)
        assertTrue(unknownMatches.none { it.category == "NOTE" })
    }
}
