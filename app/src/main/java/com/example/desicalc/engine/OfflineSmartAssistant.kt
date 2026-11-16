package com.example.desicalc.engine

import com.example.desicalc.data.local.TransactionEntity
import com.example.desicalc.data.repository.DesiLedgerRepository
import com.example.desicalc.model.CustomerLedgerSummary
import com.example.desicalc.model.PendingTransactionAction
import com.example.desicalc.model.ReportPeriod
import com.example.desicalc.model.TransactionType
import com.example.desicalc.model.formatInr
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

data class SmartAssistantOutcome(
    val replyText: String,
    val pendingAction: PendingTransactionAction? = null,
    val engineName: String = "OFFLINE_ENGINE"
)

class OfflineSmartAssistant(
    private val repository: DesiLedgerRepository
) {
    suspend fun processQuery(query: String): SmartAssistantOutcome {
        val raw = query.trim()
        val lower = raw.lowercase(Locale.ROOT)
        val summaries = repository.getAllSummariesOnce()
        val allTransactions = repository.getAllTransactionsOnce()

        // 1. Check natural language transaction intent (Hindi / Hinglish / English)
        val txIntent = tryParseTransactionCommand(raw, lower, summaries)
        if (txIntent != null) {
            return txIntent
        }

        // 2. Today's summary ("Aaj ka total hisab batao" / "Today's summary")
        if (containsAny(lower, listOf("aaj ka", "today", "aaj", "today's")) &&
            containsAny(lower, listOf("hisab", "total", "summary", "report", "udhar", "jama", "batao"))
        ) {
            val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val todayTx = allTransactions.filter { it.timestamp >= todayStart }
            val udharToday = todayTx.filter {
                val t = TransactionType.fromCode(it.type)
                t == TransactionType.UDHAR_DIYA || t == TransactionType.UDHAR_LIYA
            }.sumOf { it.amount }
            val jamaToday = todayTx.filter {
                val t = TransactionType.fromCode(it.type)
                t == TransactionType.JAMA_RECEIVED || t == TransactionType.PAYMENT_MADE
            }.sumOf { it.amount }
            val totalRemaining = summaries.sumOf { maxOf(0.0, it.netBalance) }
            val reply = buildString {
                appendLine("Aaj ka Total Hisab (Today's Ledger):")
                appendLine("• Aaj ke transactions: ${todayTx.size}")
                appendLine("• Aaj Udhar: ${formatInr(udharToday)}")
                appendLine("• Aaj Jama: ${formatInr(jamaToday)}")
                append("• Kul Baki (Total Receivable): ${formatInr(totalRemaining)}")
            }
            return SmartAssistantOutcome(replyText = reply)
        }

        // 3. Highest pending balance ("Kis customer ka sabse zyada baki hai?" / "highest due")
        if (containsAny(lower, listOf("sabse zyada", "highest", "most due", "top baki", "zyada baki", "max balance", "kis customer"))) {
            val topCustomer = summaries
                .filter { it.netBalance > 0.01 }
                .maxByOrNull { it.netBalance }
            return if (topCustomer != null) {
                val mobileInfo = if (topCustomer.mobile.isNotBlank()) " (${topCustomer.mobile})" else ""
                SmartAssistantOutcome(
                    replyText = "Sabse zyada baki ${topCustomer.name}$mobileInfo ka hai: ${formatInr(topCustomer.netBalance)} (Udhar: ${formatInr(topCustomer.udharDiyaTotal)}, Jama: ${formatInr(topCustomer.jamaReceivedTotal)})."
                )
            } else {
                SmartAssistantOutcome(
                    replyText = "Abhi kisi bhi customer par koi baki (receivable) nahi hai. Sabhi accounts clear hain!"
                )
            }
        }

        // 4. Monthly / Weekly / Previous month report ("Pichhle mahine ki report dikhao")
        if (containsAny(lower, listOf("mahine", "month", "pichhle", "weekly", "hafte", "report"))) {
            val isWeekly = containsAny(lower, listOf("week", "hafte", "saptah"))
            val report = repository.buildPeriodReport(
                allTransactions,
                if (isWeekly) ReportPeriod.WEEKLY else ReportPeriod.MONTHLY
            )
            val reply = buildString {
                appendLine("${report.periodTitle} Report:")
                appendLine("• Kul Transactions: ${report.transactionCount}")
                appendLine("• Udhar Diya: ${formatInr(report.udharDiyaTotal)}")
                appendLine("• Jama Received: ${formatInr(report.jamaReceivedTotal)}")
                if (report.udharLiyaTotal > 0 || report.paymentMadeTotal > 0) {
                    appendLine("• Udhar Liya: ${formatInr(report.udharLiyaTotal)} | Paid: ${formatInr(report.paymentMadeTotal)}")
                }
                append("• Net Cash Flow: ${formatInr(report.netFlow)}")
            }
            return SmartAssistantOutcome(replyText = reply)
        }

        // 5. Customer-specific balance lookup ("Rahul ka hisab / balance")
        val matchedCustomer = findBestMatchingCustomer(lower, summaries)
        if (matchedCustomer != null && containsAny(lower, listOf("balance", "baki", "hisab", "kitna", "status", "ledger"))) {
            val statusText = when {
                matchedCustomer.netBalance > 0.01 -> "Lena hai (Baki): ${formatInr(matchedCustomer.netBalance)}"
                matchedCustomer.netBalance < -0.01 -> "Dena hai (Payable): ${formatInr(abs(matchedCustomer.netBalance))}"
                else -> "Hisab Clear (₹0)"
            }
            return SmartAssistantOutcome(
                replyText = "${matchedCustomer.name} ka Khata:\n• Udhar Diya: ${formatInr(matchedCustomer.udharDiyaTotal)}\n• Jama Mila: ${formatInr(matchedCustomer.jamaReceivedTotal)}\n• Net Status: $statusText"
            )
        }

        // 6. Natural language GST / Discount / Percentage / Arithmetic
        val calcReply = tryEvaluateMathQuery(raw, lower)
        if (calcReply != null) {
            return SmartAssistantOutcome(replyText = calcReply)
        }

        // 7. Helpful bilingual guidance
        return SmartAssistantOutcome(
            replyText = "Main aapki Hindi aur English dono mein madad kar sakta hoon:\n" +
                "• \"Rahul ko 500 rupaye udhar diye\"\n" +
                "• \"Rahul se 200 rupaye jama mile\"\n" +
                "• \"Aaj ka total hisab batao\"\n" +
                "• \"Kis customer ka sabse zyada baki hai?\"\n" +
                "• \"Pichhle mahine ki report dikhao\"\n" +
                "• \"18% GST on 2500\" ya \"20% discount on 1500\""
        )
    }

    private suspend fun tryParseTransactionCommand(
        raw: String,
        lower: String,
        summaries: List<CustomerLedgerSummary>
    ): SmartAssistantOutcome? {
        val amountMatch = Regex("""(\d+(?:\.\d{1,2})?)""").find(lower) ?: return null
        val amount = amountMatch.groupValues[1].toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val txType = when {
            containsAny(lower, listOf("udhar liya", "borrowed", "liya udhar")) -> TransactionType.UDHAR_LIYA
            containsAny(lower, listOf("payment made", "chukaya", "wapas diye", "paid back")) -> TransactionType.PAYMENT_MADE
            containsAny(lower, listOf("jama", "mila", "mile", "received", "wapas aaye", "paid me")) -> TransactionType.JAMA_RECEIVED
            containsAny(lower, listOf("udhar", "diye", "diya", "credit", "gave", "baki likh")) -> TransactionType.UDHAR_DIYA
            else -> return null
        }

        val matched = findBestMatchingCustomer(lower, summaries)
        val extractedName = matched?.name ?: extractCandidateName(raw)
        if (extractedName.isBlank()) {
            return SmartAssistantOutcome(
                replyText = "Kripya customer ka naam bhi likhein (e.g., \"Rahul ko ${amount.toInt()} rupaye udhar diye\")."
            )
        }

        val dupTx: TransactionEntity? = if (matched != null) {
            repository.checkPossibleDuplicate(matched.id, txType, amount, windowMinutes = 15L)
        } else null

        val dupNote = if (dupTx != null) {
            "Warning: Same ${txType.labelEn} of ${formatInr(amount)} for ${matched?.name} was recorded recently at ${DesiLedgerRepository.formatTimestamp(dupTx.timestamp)}."
        } else null

        val action = PendingTransactionAction(
            customerName = extractedName,
            matchedCustomerId = matched?.id,
            customerMobile = matched?.mobile.orEmpty(),
            transactionType = txType.code,
            amount = amount,
            description = "AI Entry: $raw",
            isPossibleDuplicate = dupTx != null,
            duplicateReason = dupNote,
            isNewCustomer = matched == null
        )

        val confirmMsg = buildString {
            append("Maine samjha: ${action.customerName} ke liye ${formatInr(amount)} (${txType.labelEn}). ")
            if (action.isNewCustomer) {
                append("Yeh naya customer add hoga. ")
            }
            if (dupNote != null) {
                append("\n⚠️ $dupNote ")
            }
            append("\nKripya save karne se pehle neeche 'Confirm & Save' dabayein.")
        }

        return SmartAssistantOutcome(
            replyText = confirmMsg,
            pendingAction = action
        )
    }

    private fun tryEvaluateMathQuery(raw: String, lower: String): String? {
        // GST query: "18% gst on 2500" or "2500 par 18% gst"
        if (lower.contains("gst")) {
            val nums = Regex("""\d+(?:\.\d+)?""").findAll(lower).map { it.value.toDouble() }.toList()
            if (nums.size >= 2) {
                val rate = nums.firstOrNull { it <= 40.0 } ?: nums[0]
                val amount = nums.lastOrNull { it != rate } ?: nums[1]
                val inclusive = containsAny(lower, listOf("inclusive", "included", "nikalo", "extract"))
                val res = CalculatorEngine.calculateGst(amount, rate, inclusive)
                return "GST Calculation (${rate}% ${if (inclusive) "Inclusive" else "Exclusive"}):\n" +
                    "• Base Amount: ${formatInr(res.baseAmount)}\n" +
                    "• CGST (${rate / 2}%): ${formatInr(res.cgstAmount)} | SGST (${rate / 2}%): ${formatInr(res.sgstAmount)}\n" +
                    "• Total: ${formatInr(res.totalAmount)}"
            }
        }

        // Discount query: "20% discount on 1500"
        if (containsAny(lower, listOf("discount", "chhoot", "choot", "off"))) {
            val nums = Regex("""\d+(?:\.\d+)?""").findAll(lower).map { it.value.toDouble() }.toList()
            if (nums.size >= 2) {
                val pct = nums.firstOrNull { it <= 99.0 } ?: nums[0]
                val mrp = nums.lastOrNull { it != pct } ?: nums[1]
                val res = CalculatorEngine.calculateDiscount(mrp, pct)
                return "Discount Calculation (${pct}% off on ${formatInr(mrp)}):\n" +
                    "• Savings: ${formatInr(res.totalDiscountAmount)}\n" +
                    "• Final Price: ${formatInr(res.finalPrice)}"
            }
        }

        // Direct arithmetic expression inside query
        val exprCandidate = raw.replace(Regex("[^0-9+\\-*/().%]"), "").trim()
        if (exprCandidate.any { it in listOf('+', '-', '*', '/') } && exprCandidate.any { it.isDigit() }) {
            val result = CalculatorEngine.evaluateExpression(exprCandidate)
            if (result.isSuccess) {
                val value = result.getOrThrow()
                return "Hisab Result: $exprCandidate = ${CalculatorEngine.formatCleanNumber(value)}"
            }
        }
        return null
    }

    private fun findBestMatchingCustomer(
        lowerQuery: String,
        summaries: List<CustomerLedgerSummary>
    ): CustomerLedgerSummary? {
        return summaries
            .sortedByDescending { it.name.length }
            .firstOrNull { c ->
                val cname = c.name.lowercase(Locale.ROOT).trim()
                cname.isNotEmpty() && lowerQuery.contains(cname)
            }
    }

    private fun extractCandidateName(raw: String): String {
        val cleaned = raw
            .replace(Regex("""\d+(?:\.\d+)?"""), " ")
            .replace(
                Regex(
                    """\b(ko|se|ka|ki|ke|for|from|to|rs|inr|rupaye|rupees|rupee|udhar|jama|diye|diya|liya|mila|mile|received|gave|credit|debit|payment|made|add|record|please|karo|do)\b""",
                    RegexOption.IGNORE_CASE
                ),
                " "
            )
            .replace(Regex("""[^\p{L}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return cleaned.split(" ")
            .filter { it.length >= 2 }
            .take(3)
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it) }
}
