package com.example

import com.example.service.PureLockAccessibilityService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionGracePeriodTest {

    @Before
    fun setUp() {
        PureLockAccessibilityService.clearAllSessions()
    }

    @Test
    fun testPackageNeverUnlocked_isInvalid() {
        assertFalse(PureLockAccessibilityService.isPackageSessionValid("com.example.test", 30000L))
    }

    @Test
    fun testPackageUnlocked_activeInForeground_isValid() {
        val testPkg = "com.example.bank"
        PureLockAccessibilityService.onPackageUnlocked(testPkg)

        // User is currently active in app (never navigated away)
        assertTrue(PureLockAccessibilityService.isPackageSessionValid(testPkg, 30000L))
        assertTrue(PureLockAccessibilityService.isPackageSessionValid(testPkg, 0L))
        assertTrue(PureLockAccessibilityService.isPackageSessionValid(testPkg, -1L))
    }

    @Test
    fun testClearAllSessions_revokesAllAccess() {
        val testPkg1 = "com.example.bank"
        val testPkg2 = "com.example.chat"
        PureLockAccessibilityService.onPackageUnlocked(testPkg1)
        PureLockAccessibilityService.onPackageUnlocked(testPkg2)

        assertTrue(PureLockAccessibilityService.isPackageSessionValid(testPkg1, 30000L))
        assertTrue(PureLockAccessibilityService.isPackageSessionValid(testPkg2, 30000L))

        PureLockAccessibilityService.clearAllSessions()

        assertFalse(PureLockAccessibilityService.isPackageSessionValid(testPkg1, 30000L))
        assertFalse(PureLockAccessibilityService.isPackageSessionValid(testPkg2, 30000L))
    }
}
