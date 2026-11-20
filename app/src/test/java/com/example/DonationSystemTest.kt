package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.view.ContextThemeWrapper
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.example.service.BillingGateway
import com.example.service.DonationBillingManager
import com.example.service.DonationTier
import com.example.service.findActivity
import com.example.util.DonationChannelsHelper
import com.example.util.DonationEngagementManager
import com.example.util.DonationEngagementState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DonationSystemTest {

    private class FakeBillingGateway : BillingGateway {
        override var isReady: Boolean = false
        var startConnectionCalls: Int = 0
        var queryCatalogCalls: Int = 0
        var queryPurchasesCalls: Int = 0
        var endConnectionCalls: Int = 0

        override fun startConnection(onSetupFinished: (Boolean) -> Unit, onDisconnected: () -> Unit) {
            startConnectionCalls++
            isReady = true
            onSetupFinished(true)
        }

        override fun queryProductDetails(
            productIds: List<String>,
            onResult: (BillingResult, List<ProductDetails>) -> Unit
        ) {
            queryCatalogCalls++
            val ok = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
            onResult(ok, emptyList())
        }

        override fun queryOwnedPurchases(onResult: (BillingResult, List<Purchase>) -> Unit) {
            queryPurchasesCalls++
            val ok = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
            onResult(ok, emptyList())
        }

        override fun acknowledgePurchase(purchaseToken: String, onResult: (BillingResult) -> Unit) {
            val ok = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
            onResult(ok)
        }

        override fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
            return BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        }

        override fun endConnection() {
            endConnectionCalls++
            isReady = false
        }
    }

    @Test
    fun `banner and prompts never show at install or first launch`() {
        val now = 10L * DonationEngagementManager.DAY_MS
        val freshUser = DonationEngagementState(
            firstInstallTimestamp = now,
            appLaunchCount = 1,
            successfulOperationsCount = 0
        )
        assertFalse(DonationEngagementManager.shouldShowHomeBanner(freshUser, now))
        assertFalse(DonationEngagementManager.shouldShowPostOperationPrompt(freshUser, false, now))

        // Even after several operations on day 0, install age safeguard still suppresses prompts
        val dayZeroActive = freshUser.copy(appLaunchCount = 5, successfulOperationsCount = 10)
        assertFalse(DonationEngagementManager.shouldShowHomeBanner(dayZeroActive, now + 1000L))
        assertFalse(DonationEngagementManager.shouldShowPostOperationPrompt(dayZeroActive, false, now + 1000L))
    }

    @Test
    fun `home banner appears after verified utility and respects 14-day cooldown`() {
        val installTime = 1_000_000L
        val afterTwoDays = installTime + (2L * DonationEngagementManager.DAY_MS) + 10_000L

        val qualifiedState = DonationEngagementState(
            firstInstallTimestamp = installTime,
            appLaunchCount = 3,
            successfulOperationsCount = 3
        )
        assertTrue(DonationEngagementManager.shouldShowHomeBanner(qualifiedState, afterTwoDays))

        // Dismissing silences banner for 14 days
        val dismissedState = qualifiedState.copy(donationBannerDismissedAt = afterTwoDays)
        val day13Later = afterTwoDays + (13L * DonationEngagementManager.DAY_MS)
        val day14Later = afterTwoDays + (14L * DonationEngagementManager.DAY_MS)
        assertFalse(DonationEngagementManager.shouldShowHomeBanner(dismissedState, day13Later))
        assertTrue(DonationEngagementManager.shouldShowHomeBanner(dismissedState, day14Later))
    }

    @Test
    fun `post operation prompt enforces 7 ops threshold, session once, 30d cooldown, and 3 lifetime cap`() {
        val installTime = 1_000_000L
        val readyTime = installTime + (5L * DonationEngagementManager.DAY_MS)

        val underOps = DonationEngagementState(
            firstInstallTimestamp = installTime,
            appLaunchCount = 4,
            successfulOperationsCount = 6
        )
        assertFalse(DonationEngagementManager.shouldShowPostOperationPrompt(underOps, false, readyTime))

        val ready = underOps.copy(successfulOperationsCount = 7)
        assertTrue(DonationEngagementManager.shouldShowPostOperationPrompt(ready, sessionAlreadyPrompted = false, readyTime))
        // Never prompt twice in same session
        assertFalse(DonationEngagementManager.shouldShowPostOperationPrompt(ready, sessionAlreadyPrompted = true, readyTime))

        // Respect 30-day cooldown
        val recentlyPrompted = ready.copy(
            lastDonationPromptTimestamp = readyTime,
            donationPromptShownCount = 1
        )
        assertFalse(
            DonationEngagementManager.shouldShowPostOperationPrompt(
                recentlyPrompted,
                sessionAlreadyPrompted = false,
                nowMs = readyTime + (29L * DonationEngagementManager.DAY_MS)
            )
        )
        assertTrue(
            DonationEngagementManager.shouldShowPostOperationPrompt(
                recentlyPrompted,
                sessionAlreadyPrompted = false,
                nowMs = readyTime + (30L * DonationEngagementManager.DAY_MS)
            )
        )

        // Respect max 3 lifetime prompts
        val maxedOut = ready.copy(
            lastDonationPromptTimestamp = 0L,
            donationPromptShownCount = DonationEngagementManager.MAX_LIFETIME_AUTO_PROMPTS
        )
        assertFalse(DonationEngagementManager.shouldShowPostOperationPrompt(maxedOut, false, readyTime))
    }

    @Test
    fun `active supporter permanently suppresses all banners and prompts`() {
        val now = 100L * DonationEngagementManager.DAY_MS
        val supporterState = DonationEngagementState(
            firstInstallTimestamp = 1_000L,
            appLaunchCount = 50,
            successfulOperationsCount = 100,
            isActiveSupporter = true,
            supporterTier = "purelock_support_tier_2"
        )
        assertFalse(DonationEngagementManager.shouldShowHomeBanner(supporterState, now))
        assertFalse(DonationEngagementManager.shouldShowPostOperationPrompt(supporterState, false, now))
    }

    @Test
    fun `milestone detection catches exact milestone crossings`() {
        assertEquals(7, DonationEngagementManager.getMilestoneReached(6, 7))
        assertEquals(10, DonationEngagementManager.getMilestoneReached(9, 10))
        assertEquals(25, DonationEngagementManager.getMilestoneReached(24, 25))
        assertNull(DonationEngagementManager.getMilestoneReached(10, 11))
    }

    @Test
    fun `context findActivity unwraps nested ContextWrappers and rejects Application or finishing Activity`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        assertNull(appContext.findActivity())

        val activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val liveActivity = activityController.get()

        // Simulate Compose Dialog ContextThemeWrapper(ContextWrapper(Activity)) nesting
        val outerWrapper = ContextWrapper(liveActivity)
        val themedWrapper = ContextThemeWrapper(outerWrapper, android.R.style.Theme_Material_Light)

        assertSame(liveActivity, themedWrapper.findActivity())

        liveActivity.finish()
        assertNull(themedWrapper.findActivity())
    }

    @Test
    fun `lifecycle-aware billing manager tracks Activity session across lifecycle transitions and release`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val fakeGateway = FakeBillingGateway()

        val manager = DonationBillingManager(
            context = appContext,
            coroutineScope = scope,
            ownsScope = false,
            ioDispatcher = Dispatchers.Unconfined,
            mainDispatcher = Dispatchers.Unconfined,
            gatewayFactory = { _, _ -> fakeGateway },
            onSupporterVerified = {}
        )

        // Before binding an Activity session, launching without a host reports a safe UI error instead of crashing
        assertFalse(manager.hasBoundActiveSession())
        manager.launchDonationFlow(DonationTier.TIER_1)
        assertTrue(manager.uiState.value.isError)
        assertEquals(
            appContext.getString(R.string.donation_play_no_activity_error),
            manager.uiState.value.statusMessage
        )
        manager.clearStatusMessage()

        // Bind a live FragmentActivity and drive ON_START -> ON_RESUME -> ON_STOP -> ON_DESTROY
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).create().start()
        val activity = controller.get()
        manager.bindActivitySession(activity)

        assertTrue(manager.hasBoundActiveSession())
        assertTrue(fakeGateway.startConnectionCalls >= 1)
        assertTrue(fakeGateway.queryCatalogCalls >= 1)
        assertTrue(fakeGateway.queryPurchasesCalls >= 1)

        val purchasesBeforeResume = fakeGateway.queryPurchasesCalls
        controller.resume()
        assertTrue(fakeGateway.queryPurchasesCalls > purchasesBeforeResume)

        // When Activity stops or is destroyed, session is no longer active
        controller.pause().stop()
        assertFalse(manager.hasBoundActiveSession())
        controller.destroy()
        assertFalse(manager.hasBoundActiveSession())

        manager.release()
        assertEquals(1, fakeGateway.endConnectionCalls)
        scope.cancel()
    }

    @Test
    fun `paypal and kofi urls format cleanly and qr generator creates black and white matrix`() {
        assertEquals("https://paypal.me/purelockdev/3USD", DonationChannelsHelper.buildPayPalUrl(3.0))
        assertEquals("https://paypal.me/purelockdev", DonationChannelsHelper.buildPayPalUrl(null))
        assertEquals("https://paypal.me/purelockdev/4.99USD", DonationChannelsHelper.buildPayPalUrl(4.99))
        assertEquals("https://ko-fi.com/purelockdev", DonationChannelsHelper.buildKofiUrl())

        val bitmap = DonationChannelsHelper.generateQrCodeBitmap(
            content = DonationChannelsHelper.buildPayPalUrl(5.0),
            sizePx = 256
        )
        assertNotNull(bitmap)
        assertEquals(256, bitmap!!.width)
        assertEquals(256, bitmap.height)

        var hasBlack = false
        var hasWhite = false
        for (y in 0 until bitmap.height step 4) {
            for (x in 0 until bitmap.width step 4) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel == Color.BLACK) hasBlack = true
                if (pixel == Color.WHITE) hasWhite = true
                if (hasBlack && hasWhite) break
            }
            if (hasBlack && hasWhite) break
        }
        assertTrue(hasBlack && hasWhite)
    }
}
