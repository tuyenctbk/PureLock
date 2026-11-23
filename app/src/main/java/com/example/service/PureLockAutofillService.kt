package com.example.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.R
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import com.example.data.model.EncryptedVaultEntity
import com.example.ui.LockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(Build.VERSION_CODES.O)
class PureLockAutofillService : AutofillService() {

    companion object {
        private val CREDENTIAL_CATEGORIES = setOf("PASSWORD", "PIN", "BANK_PIN", "CODE_2FA", "CREDENTIAL")
        private val authRequestCodeCounter = AtomicInteger(10_000)

        /**
         * Ranks vault items for the target web domain / package name.
         * Never leaks generic notes or unrelated secrets when direct domain/package matches exist.
         */
        internal fun selectAutofillCandidates(
            vaultItems: List<EncryptedVaultEntity>,
            webDomains: Set<String>,
            targetPackage: String,
            maxResults: Int = 5
        ): List<EncryptedVaultEntity> {
            val normalizedPkg = targetPackage.lowercase().trim()
            val pkgToken = normalizedPkg.substringAfterLast('.')
            val normalizedDomains = webDomains.map { it.lowercase().trim() }.filter { it.isNotBlank() }

            val scored = vaultItems.map { item ->
                val itemSite = item.websiteOrApp.lowercase()
                val itemTitle = item.title.lowercase()
                val catUpper = item.category.uppercase()
                var matchScore = 0
                if (normalizedDomains.any { d -> itemSite.contains(d) || itemTitle.contains(d) }) {
                    matchScore += 100
                }
                if (normalizedPkg.isNotBlank()) {
                    if (itemSite.contains(normalizedPkg) ||
                        (pkgToken.length >= 3 && (itemSite.contains(pkgToken) || itemTitle.contains(pkgToken)))
                    ) {
                        matchScore += 50
                    }
                }
                val isCredentialCategory = catUpper in CREDENTIAL_CATEGORIES
                val rankScore = matchScore + (if (isCredentialCategory) 20 else 0) + (if (item.isFavorite) 5 else 0)
                Triple(item, matchScore, rankScore)
            }

            // If we have exact domain/package matches, restrict strictly to those matches
            val directMatches = scored.filter { it.second > 0 }
                .sortedByDescending { it.third }
                .map { it.first }
            if (directMatches.isNotEmpty()) {
                return directMatches.take(maxResults)
            }

            // Otherwise only offer credential-type entries (never raw personal notes) behind authentication
            return scored
                .filter { it.first.category.uppercase() in CREDENTIAL_CATEGORIES }
                .sortedByDescending { it.third }
                .map { it.first }
                .take(maxResults)
        }

        /**
         * Builds the unlocked FillResponse containing matching encrypted credential datasets.
         * Must ONLY be called after Master Lock authentication succeeds in LockOverlayActivity.
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun buildAuthenticatedFillResponse(
            context: Context,
            vaultItems: List<EncryptedVaultEntity>,
            usernameIds: List<AutofillId>,
            passwordIds: List<AutofillId>,
            webDomains: Set<String>,
            targetPackage: String
        ): FillResponse? {
            if (usernameIds.isEmpty() && passwordIds.isEmpty()) return null
            val candidates = selectAutofillCandidates(vaultItems, webDomains, targetPackage)
            if (candidates.isEmpty()) return null

            val fillResponseBuilder = FillResponse.Builder()
            candidates.forEach { item ->
                val localizedCategory = when (item.category.uppercase()) {
                    "PASSWORD" -> context.getString(R.string.vault_filter_passwords)
                    "PIN", "BANK_PIN" -> context.getString(R.string.vault_filter_pins)
                    "CODE_2FA" -> context.getString(R.string.vault_filter_2fa)
                    "CARD" -> context.getString(R.string.vault_filter_cards)
                    "NOTE" -> context.getString(R.string.vault_filter_notes)
                    "WORK" -> context.getString(R.string.category_work)
                    "PERSONAL" -> context.getString(R.string.category_personal)
                    "FINANCE" -> context.getString(R.string.category_finance)
                    "CREDENTIAL" -> context.getString(R.string.category_credentials)
                    "API_KEY" -> context.getString(R.string.category_api_keys)
                    "RECOVERY_KEY" -> context.getString(R.string.category_recovery_keys)
                    else -> item.category
                }
                val presentation = RemoteViews(context.packageName, android.R.layout.activity_list_item).apply {
                    setImageViewResource(android.R.id.icon, R.drawable.splash_logo)
                    setTextViewText(
                        android.R.id.text1,
                        context.getString(R.string.autofill_dataset_label, item.title, localizedCategory)
                    )
                }

                @Suppress("DEPRECATION")
                val datasetBuilder = Dataset.Builder(presentation)

                if (usernameIds.isNotEmpty()) {
                    val userValue = if (item.username.isNotBlank()) item.username else item.title
                    usernameIds.forEach { id ->
                        @Suppress("DEPRECATION")
                        datasetBuilder.setValue(id, AutofillValue.forText(userValue))
                    }
                }

                if (passwordIds.isNotEmpty()) {
                    passwordIds.forEach { id ->
                        @Suppress("DEPRECATION")
                        datasetBuilder.setValue(id, AutofillValue.forText(item.secretContent))
                    }
                }

                fillResponseBuilder.addDataset(datasetBuilder.build())
            }
            return fillResponseBuilder.build()
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val contexts = request.fillContexts
        if (contexts.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val lastContext = contexts.lastOrNull()
        val structure = lastContext?.structure
        if (structure == null || structure.windowNodeCount == 0) {
            callback.onSuccess(null)
            return
        }

        val targetPackage = structure.activityComponent?.packageName?.lowercase() ?: ""
        // Never offer autofill inside PureLock's own lock/vault UI
        if (targetPackage == applicationContext.packageName.lowercase()) {
            callback.onSuccess(null)
            return
        }

        val webDomains = mutableSetOf<String>()
        val usernameIds = mutableListOf<AutofillId>()
        val passwordIds = mutableListOf<AutofillId>()

        // Inspect every window node in the AssistStructure (handles dialogs, popups, and WebViews)
        for (i in 0 until structure.windowNodeCount) {
            val rootNode = structure.getWindowNodeAt(i)?.rootViewNode ?: continue
            findAutofillNodes(rootNode, usernameIds, passwordIds, webDomains)
        }

        if (usernameIds.isEmpty() && passwordIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val job = serviceScope.launch {
            try {
                if (cancellationSignal.isCanceled) return@launch
                val db = PureLockDatabase.getDatabase(applicationContext)
                val activeItems = db.encryptedVaultDao().getActiveVaultItems().first()
                val hasCandidates = selectAutofillCandidates(
                    vaultItems = activeItems,
                    webDomains = webDomains,
                    targetPackage = targetPackage
                ).isNotEmpty()
                if (cancellationSignal.isCanceled || !hasCandidates) {
                    if (!cancellationSignal.isCanceled) callback.onSuccess(null)
                    return@launch
                }

                // Always gate vault credentials behind Master Lock authentication before revealing datasets
                val allAutofillIds = (usernameIds + passwordIds).distinct().toTypedArray()
                val authIntent = Intent(applicationContext, LockOverlayActivity::class.java).apply {
                    putExtra(LockOverlayActivity.EXTRA_LOCKED_PACKAGE, targetPackage.ifBlank { applicationContext.packageName })
                    putExtra(LockOverlayActivity.EXTRA_AUTOFILL_AUTH_REQUEST, true)
                    putParcelableArrayListExtra(LockOverlayActivity.EXTRA_AUTOFILL_USERNAME_IDS, ArrayList(usernameIds))
                    putParcelableArrayListExtra(LockOverlayActivity.EXTRA_AUTOFILL_PASSWORD_IDS, ArrayList(passwordIds))
                    putStringArrayListExtra(LockOverlayActivity.EXTRA_AUTOFILL_WEB_DOMAINS, ArrayList(webDomains))
                    putExtra(LockOverlayActivity.EXTRA_AUTOFILL_TARGET_PACKAGE, targetPackage)
                }

                val pendingIntentFlags = PendingIntent.FLAG_CANCEL_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val requestCode = authRequestCodeCounter.incrementAndGet() and 0x7FFFFFFF
                val authSender = PendingIntent.getActivity(
                    applicationContext,
                    requestCode,
                    authIntent,
                    pendingIntentFlags
                ).intentSender

                val authPresentation = RemoteViews(packageName, android.R.layout.activity_list_item).apply {
                    setImageViewResource(android.R.id.icon, R.drawable.splash_logo)
                    setTextViewText(android.R.id.text1, getString(R.string.autofill_service_label))
                }

                @Suppress("DEPRECATION")
                val lockedResponse = FillResponse.Builder()
                    .setAuthentication(allAutofillIds, authSender, authPresentation)
                    .build()

                if (!cancellationSignal.isCanceled) {
                    callback.onSuccess(lockedResponse)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!cancellationSignal.isCanceled) {
                    callback.onSuccess(null)
                }
            }
        }

        cancellationSignal.setOnCancelListener {
            job.cancel()
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun findAutofillNodes(
        node: android.app.assist.AssistStructure.ViewNode,
        usernameIds: MutableList<AutofillId>,
        passwordIds: MutableList<AutofillId>,
        webDomains: MutableSet<String>
    ) {
        node.webDomain?.lowercase()?.takeIf { it.isNotBlank() }?.let { webDomains.add(it) }

        val autofillHints = node.autofillHints
        val hint = node.hint?.lowercase() ?: ""
        val idEntry = node.idEntry?.lowercase() ?: ""

        val isPassword = (autofillHints?.contains(android.view.View.AUTOFILL_HINT_PASSWORD) == true) ||
                hint.contains("pass") || idEntry.contains("pass")

        val isUsername = !isPassword && (
                (autofillHints?.contains(android.view.View.AUTOFILL_HINT_USERNAME) == true) ||
                        (autofillHints?.contains(android.view.View.AUTOFILL_HINT_EMAIL_ADDRESS) == true) ||
                        hint.contains("user") || hint.contains("email") || idEntry.contains("user") || idEntry.contains("email")
                )

        val autofillId = node.autofillId
        if (autofillId != null) {
            if (isPassword) {
                passwordIds.add(autofillId)
            } else if (isUsername) {
                usernameIds.add(autofillId)
            }
        }

        for (i in 0 until node.childCount) {
            findAutofillNodes(node.getChildAt(i), usernameIds, passwordIds, webDomains)
        }
    }
}
