package com.example.desicalc.engine

import android.util.Log
import com.example.desicalc.data.repository.DesiLedgerRepository
import com.example.desicalc.model.PendingTransactionAction
import com.example.desicalc.model.TransactionType
import com.example.desicalc.model.formatInr
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

private const val TAG = "OnlineGeminiAssistant"

class OnlineGeminiAssistant(
    private val repository: DesiLedgerRepository
) {
    private val responseSchema = Schema.obj(
        properties = mapOf(
            "replyText" to Schema.string(
                description = "Helpful response in the user's language (Hindi, Hinglish, or English). Never claim a financial record was already saved; ask for confirmation if proposing a transaction."
            ),
            "intent" to Schema.enumeration(
                values = listOf("PROPOSE_TRANSACTION", "QUERY_ANSWER", "CALCULATION", "GENERAL"),
                description = "Classified user intent"
            ),
            "customerName" to Schema.string(
                description = "Customer name if proposing a transaction, otherwise empty string",
                nullable = true
            ),
            "transactionType" to Schema.enumeration(
                values = listOf("UDHAR_DIYA", "JAMA_RECEIVED", "UDHAR_LIYA", "PAYMENT_MADE", "NONE"),
                description = "Exact transaction type if intent is PROPOSE_TRANSACTION, otherwise NONE"
            ),
            "amount" to Schema.double(
                description = "Positive numeric transaction amount in INR if proposing a transaction, else 0",
                nullable = true
            ),
            "description" to Schema.string(
                description = "Short transaction note/description",
                nullable = true
            )
        ),
        optionalProperties = listOf("customerName", "amount", "description")
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@OnlineGeminiAssistant.responseSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun askGemini(userQuery: String): Result<SmartAssistantOutcome> {
        return try {
            val summaries = repository.getAllSummariesOnce()
            val allTransactions = repository.getAllTransactionsOnce()
            val todayReport = repository.buildPeriodReport(allTransactions, com.example.desicalc.model.ReportPeriod.DAILY)
            val monthlyReport = repository.buildPeriodReport(allTransactions, com.example.desicalc.model.ReportPeriod.MONTHLY)

            val topCustomersContext = summaries
                .sortedByDescending { kotlin.math.abs(it.netBalance) }
                .take(15)
                .joinToString("\n") { c ->
                    "- ${c.name} (ID:${c.id}, NetBalance:${formatInr(c.netBalance)}, UdharDiya:${formatInr(c.udharDiyaTotal)}, Jama:${formatInr(c.jamaReceivedTotal)})"
                }.ifBlank { "No customers recorded yet." }

            val prompt = """
                You are the Desi Calculator AI PRO V2.0 bilingual (Hindi, Hinglish, English) Khata & Business Calculator Assistant.
                RULES:
                1. NEVER invent fake customer records or pretend a transaction has already been saved.
                2. If the user asks to record Udhar or Jama (e.g., "Rahul ko 500 rupaye udhar diye", "Rahul se 200 jama mile"), set intent="PROPOSE_TRANSACTION", fill customerName, transactionType, amount, description, and ask the user to confirm in replyText.
                3. Never mix UDHAR_DIYA/JAMA_RECEIVED with UDHAR_LIYA/PAYMENT_MADE.
                4. Use INR ₹ formatting for monetary figures.

                LIVE LEDGER SNAPSHOT:
                Total Customers: ${summaries.size}
                Today: ${todayReport.transactionCount} entries | Udhar: ${formatInr(todayReport.udharDiyaTotal)} | Jama: ${formatInr(todayReport.jamaReceivedTotal)}
                Last 30 Days: ${monthlyReport.transactionCount} entries | Udhar: ${formatInr(monthlyReport.udharDiyaTotal)} | Jama: ${formatInr(monthlyReport.jamaReceivedTotal)} | Net Flow: ${formatInr(monthlyReport.netFlow)}
                Top Customers:
                $topCustomersContext

                USER MESSAGE:
                $userQuery
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))

            val jsonObj = JSONObject(jsonText)
            val replyText = jsonObj.optString("replyText").ifBlank { "Response received." }
            val intent = jsonObj.optString("intent", "GENERAL")

            var pendingAction: PendingTransactionAction? = null
            if (intent == "PROPOSE_TRANSACTION") {
                val cName = jsonObj.optString("customerName").trim()
                val txTypeStr = jsonObj.optString("transactionType", "NONE")
                val amount = jsonObj.optDouble("amount", 0.0)
                val desc = jsonObj.optString("description").ifBlank { "AI Entry: $userQuery" }

                if (cName.isNotEmpty() && txTypeStr != "NONE" && amount > 0.0) {
                    val txType = TransactionType.fromCode(txTypeStr)
                    val matched = summaries.firstOrNull {
                        it.name.equals(cName, ignoreCase = true)
                    } ?: summaries.firstOrNull {
                        it.name.lowercase(Locale.ROOT).contains(cName.lowercase(Locale.ROOT))
                    }

                    val dupTx = if (matched != null) {
                        repository.checkPossibleDuplicate(matched.id, txType, amount, windowMinutes = 15L)
                    } else null

                    pendingAction = PendingTransactionAction(
                        customerName = matched?.name ?: cName,
                        matchedCustomerId = matched?.id,
                        customerMobile = matched?.mobile.orEmpty(),
                        transactionType = txType.code,
                        amount = amount,
                        description = desc,
                        isPossibleDuplicate = dupTx != null,
                        duplicateReason = dupTx?.let {
                            "Possible duplicate: similar ${txType.labelEn} of ${formatInr(amount)} saved at ${DesiLedgerRepository.formatTimestamp(it.timestamp)}"
                        },
                        isNewCustomer = matched == null
                    )
                }
            }

            Result.success(
                SmartAssistantOutcome(
                    replyText = replyText,
                    pendingAction = pendingAction,
                    engineName = "GEMINI_AI_LOGIC"
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI request timed out", e)
            Result.failure(Exception("Online Gemini AI timed out after 60s. You can switch to Offline Smart Engine.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI request failed", e)
            Result.failure(e)
        }
    }
}
