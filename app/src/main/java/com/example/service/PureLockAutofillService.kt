package com.example.service

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class PureLockAutofillService : AutofillService() {

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

        val rootViewNode = structure.getWindowNodeAt(0)?.rootViewNode
        if (rootViewNode == null) {
            callback.onSuccess(null)
            return
        }

        val targetPackage = structure.activityComponent?.packageName?.lowercase() ?: ""
        val webDomains = mutableSetOf<String>()
        val usernameIds = mutableListOf<AutofillId>()
        val passwordIds = mutableListOf<AutofillId>()

        findAutofillNodes(rootViewNode, usernameIds, passwordIds, webDomains)

        if (usernameIds.isEmpty() && passwordIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                if (cancellationSignal.isCanceled) return@launch
                val db = PureLockDatabase.getDatabase(applicationContext)
                val prefs = PureLockPreferences(applicationContext)
                val repository = PureLockRepository(
                    applicationContext,
                    db.appLockDao(),
                    db.intruderDao(),
                    db.logDao(),
                    db.scheduleRuleDao(),
                    db.encryptedVaultDao(),
                    db.userSettingDao(),
                    prefs
                )

                val vaultItems = repository.activeVaultItems.first()
                if (cancellationSignal.isCanceled) return@launch
                if (vaultItems.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                // Rank matching domain/package items and credential categories first
                val rankedItems = vaultItems.sortedByDescending { item ->
                    var score = 0
                    val itemSite = item.websiteOrApp.lowercase()
                    val itemTitle = item.title.lowercase()
                    if (webDomains.any { d -> d.isNotBlank() && (itemSite.contains(d) || itemTitle.contains(d)) }) {
                        score += 100
                    }
                    if (targetPackage.isNotBlank()) {
                        val pkgToken = targetPackage.substringAfterLast('.')
                        if (itemSite.contains(targetPackage) || (pkgToken.length >= 3 && (itemSite.contains(pkgToken) || itemTitle.contains(pkgToken)))) {
                            score += 50
                        }
                    }
                    if (item.category in listOf("PASSWORD", "PIN", "CODE_2FA")) score += 20
                    if (item.isFavorite) score += 5
                    score
                }

                val fillResponseBuilder = FillResponse.Builder()

                rankedItems.take(5).forEach { item ->
                    val localizedCategory = when (item.category.uppercase()) {
                        "PASSWORD" -> getString(R.string.vault_filter_passwords)
                        "PIN", "BANK_PIN" -> getString(R.string.vault_filter_pins)
                        "CODE_2FA" -> getString(R.string.vault_filter_2fa)
                        "CARD" -> getString(R.string.vault_filter_cards)
                        "NOTE" -> getString(R.string.vault_filter_notes)
                        "WORK" -> getString(R.string.category_work)
                        "PERSONAL" -> getString(R.string.category_personal)
                        "FINANCE" -> getString(R.string.category_finance)
                        "CREDENTIAL" -> getString(R.string.category_credentials)
                        "API_KEY" -> getString(R.string.category_api_keys)
                        "RECOVERY_KEY" -> getString(R.string.category_recovery_keys)
                        else -> item.category
                    }
                    val presentation = RemoteViews(packageName, android.R.layout.activity_list_item).apply {
                        setImageViewResource(android.R.id.icon, R.drawable.splash_logo)
                        setTextViewText(
                            android.R.id.text1,
                            getString(R.string.autofill_dataset_label, item.title, localizedCategory)
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

                repository.logSecurityEvent(
                    "AUTOFILL_REQUEST",
                    "PureLock Autofill Service provided offline suggestion for ${rankedItems.take(5).size} encrypted secrets."
                )

                if (!cancellationSignal.isCanceled) {
                    callback.onSuccess(fillResponseBuilder.build())
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
