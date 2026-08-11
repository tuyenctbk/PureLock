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
        if (contexts.isNullOrEmpty()) {
            callback.onSuccess(null)
            return
        }

        val lastContext = contexts.last()
        val structure = lastContext.structure

        if (structure == null || structure.windowNodeCount <= 0) {
            callback.onSuccess(null)
            return
        }

        val usernameIds = mutableListOf<AutofillId>()
        val passwordIds = mutableListOf<AutofillId>()

        val windowNode = structure.getWindowNodeAt(0)
        val rootNode = windowNode?.rootViewNode ?: run {
            callback.onSuccess(null)
            return
        }

        findAutofillNodes(rootNode, usernameIds, passwordIds)

        if (usernameIds.isEmpty() && passwordIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PureLockDatabase.getDatabase(applicationContext)
                val prefs = PureLockPreferences(applicationContext)
                val repository = PureLockRepository(
                    applicationContext,
                    db.appLockDao(),
                    db.intruderDao(),
                    db.logDao(),
                    db.scheduleRuleDao(),
                    db.encryptedVaultDao(),
                    prefs
                )

                val vaultItems = repository.activeVaultItems.first()
                if (vaultItems.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val fillResponseBuilder = FillResponse.Builder()

                vaultItems.take(5).forEach { item ->
                    val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
                        setTextViewText(android.R.id.text1, "PureLock: ${item.title}")
                        setTextViewText(android.R.id.text2, "Category: ${item.category} (Encrypted)")
                    }

                    val datasetBuilder = Dataset.Builder(presentation)

                    if (usernameIds.isNotEmpty()) {
                        usernameIds.forEach { id ->
                            datasetBuilder.setValue(id, AutofillValue.forText(item.title))
                        }
                    }

                    if (passwordIds.isNotEmpty()) {
                        passwordIds.forEach { id ->
                            datasetBuilder.setValue(id, AutofillValue.forText(item.secretContent))
                        }
                    }

                    fillResponseBuilder.addDataset(datasetBuilder.build())
                }

                repository.logSecurityEvent(
                    "AUTOFILL_REQUEST",
                    "PureLock Autofill Service provided offline suggestion for ${vaultItems.size} encrypted secrets."
                )

                callback.onSuccess(fillResponseBuilder.build())
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    private fun findAutofillNodes(
        node: android.app.assist.AssistStructure.ViewNode,
        usernameIds: MutableList<AutofillId>,
        passwordIds: MutableList<AutofillId>
    ) {
        val autofillHints = node.autofillHints
        val hint = node.hint?.lowercase() ?: ""
        val idEntry = node.idEntry?.lowercase() ?: ""

        val isUsername = (autofillHints?.contains(android.view.View.AUTOFILL_HINT_USERNAME) == true) ||
                (autofillHints?.contains(android.view.View.AUTOFILL_HINT_EMAIL_ADDRESS) == true) ||
                hint.contains("user") || hint.contains("email") || idEntry.contains("user") || idEntry.contains("email")

        val isPassword = (autofillHints?.contains(android.view.View.AUTOFILL_HINT_PASSWORD) == true) ||
                hint.contains("pass") || idEntry.contains("pass")

        val autofillId = node.autofillId
        if (autofillId != null) {
            if (isUsername) usernameIds.add(autofillId)
            if (isPassword) passwordIds.add(autofillId)
        }

        for (i in 0 until node.childCount) {
            findAutofillNodes(node.getChildAt(i), usernameIds, passwordIds)
        }
    }
}
