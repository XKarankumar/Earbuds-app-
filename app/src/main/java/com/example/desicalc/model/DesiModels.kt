package com.example.desicalc.model

import kotlinx.serialization.Serializable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

enum class TransactionType(
    val code: String,
    val labelEn: String,
    val labelHi: String,
    val isReceivableSide: Boolean,
    val isCreditIncrease: Boolean
) {
    UDHAR_DIYA("UDHAR_DIYA", "Udhar Diya (Given)", "उधार दिया", true, true),
    JAMA_RECEIVED("JAMA_RECEIVED", "Jama Received", "जमा मिला", true, false),
    UDHAR_LIYA("UDHAR_LIYA", "Udhar Liya (Taken)", "उधार लिया", false, true),
    PAYMENT_MADE("PAYMENT_MADE", "Payment Made", "भुगतान किया", false, false);

    companion object {
        fun fromCode(code: String): TransactionType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: when (code.lowercase(Locale.ROOT).trim()) {
                    "udhar", "udhar_diya", "given", "debit" -> UDHAR_DIYA
                    "jama", "jama_received", "received", "credit" -> JAMA_RECEIVED
                    "udhar_liya", "taken", "borrowed" -> UDHAR_LIYA
                    "payment_made", "paid", "payment" -> PAYMENT_MADE
                    else -> UDHAR_DIYA
                }
        }
    }
}

enum class AppThemePreset(val id: String, val displayName: String) {
    PREMIUM_DARK("premium_dark", "Premium Dark"),
    LIGHT("light", "Light"),
    BLUE("blue", "Royal Blue"),
    GREEN("green", "Emerald Green"),
    PURPLE("purple", "Indigo Purple"),
    BLACK_AMOLED("black_amoled", "Black AMOLED"),
    CUSTOM("custom", "Custom Studio");

    companion object {
        fun fromId(id: String): AppThemePreset =
            entries.firstOrNull { it.id == id } ?: PREMIUM_DARK
    }
}

enum class CustomBackgroundStyle(val id: String, val displayName: String) {
    MIDNIGHT_SLATE("midnight_slate", "Midnight Slate"),
    DEEP_EMERALD("deep_emerald", "Deep Emerald"),
    WARM_IVORY("warm_ivory", "Warm Ivory"),
    SAFFRON_DUSK("saffron_dusk", "Saffron Dusk"),
    PURE_OBSIDIAN("pure_obsidian", "Pure Obsidian");

    companion object {
        fun fromId(id: String): CustomBackgroundStyle =
            entries.firstOrNull { it.id == id } ?: MIDNIGHT_SLATE
    }
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी"),
    HINGLISH("hinglish", "Hinglish");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class CalculatorMode(val labelEn: String, val labelHi: String) {
    BASIC("Basic", "बेसिक"),
    GST("GST", "GST"),
    DISCOUNT("Discount", "छूट"),
    PERCENTAGE("% Calc", "प्रतिशत"),
    PROFIT_LOSS("Profit/Loss", "लाभ/हानि"),
    UNIT("Units", "इकाई")
}

enum class AiEngineMode(val id: String, val label: String) {
    AUTO("auto", "Auto (Online + Offline)"),
    OFFLINE("offline", "Offline Smart Engine"),
    ONLINE("online", "Online Gemini AI")
}

enum class SmsTemplateType(val code: String, val titleEn: String, val titleHi: String) {
    REMINDER("REMINDER", "Payment Reminder", "भुगतान रिमाइंडर"),
    UDHAR_CONFIRM("UDHAR_CONFIRM", "Udhar Confirmation", "उधार पुष्टि"),
    JAMA_CONFIRM("JAMA_CONFIRM", "Jama Receipt", "जमा रसीद"),
    BALANCE_SUMMARY("BALANCE_SUMMARY", "Balance Summary", "बकाया सारांश"),
    CUSTOM("CUSTOM", "Custom Message", "कस्टम संदेश")
}

enum class ReportPeriod(val labelEn: String, val labelHi: String) {
    DAILY("Daily", "दैनिक"),
    WEEKLY("Weekly", "साप्ताहिक"),
    MONTHLY("Monthly", "मासिक")
}

enum class CustomerFilter(val labelEn: String, val labelHi: String) {
    ALL("All", "सभी"),
    LENA_HAI("To Receive", "लेना है"),
    DENA_HAI("To Pay", "देना है"),
    SETTLED("Settled", "हिसाब साफ़")
}

data class CustomerLedgerSummary(
    val id: Long,
    val customerCode: String,
    val name: String,
    val mobile: String,
    val address: String,
    val notes: String,
    val photoPath: String?,
    val autoSmsEnabled: Boolean,
    val smsConsentGranted: Boolean,
    val reminderIntervalDays: Int,
    val lastReminderSentAt: Long?,
    val udharDiyaTotal: Double,
    val jamaReceivedTotal: Double,
    val udharLiyaTotal: Double,
    val paymentMadeTotal: Double,
    val transactionCount: Int,
    val lastActivityAt: Long
) {
    /**
     * Positive = Customer owes you (Lena Hai / Receivable)
     * Negative = You owe customer (Dena Hai / Payable)
     * Receivable side and Payable side are calculated independently and never mixed.
     */
    val receivableBalance: Double get() = udharDiyaTotal - jamaReceivedTotal
    val payableBalance: Double get() = udharLiyaTotal - paymentMadeTotal
    val netBalance: Double get() = receivableBalance - payableBalance
}

data class DashboardMetrics(
    val totalCustomers: Int = 0,
    val totalUdharDiya: Double = 0.0,
    val totalJamaReceived: Double = 0.0,
    val totalUdharLiya: Double = 0.0,
    val totalPaymentMade: Double = 0.0,
    val netReceivable: Double = 0.0,
    val netPayable: Double = 0.0,
    val netRemainingBalance: Double = 0.0,
    val todayTransactionCount: Int = 0,
    val todayUdharTotal: Double = 0.0,
    val todayJamaTotal: Double = 0.0,
    val pendingCustomerCount: Int = 0
)

data class PeriodReport(
    val period: ReportPeriod,
    val periodTitle: String,
    val transactionCount: Int,
    val udharDiyaTotal: Double,
    val jamaReceivedTotal: Double,
    val udharLiyaTotal: Double,
    val paymentMadeTotal: Double,
    val netFlow: Double,
    val dailyBuckets: List<DailyChartBucket>
)

data class DailyChartBucket(
    val label: String,
    val udharAmount: Double,
    val jamaAmount: Double
)

@Serializable
data class PendingTransactionAction(
    val customerName: String,
    val matchedCustomerId: Long? = null,
    val customerMobile: String = "",
    val transactionType: String,
    val amount: Double,
    val description: String,
    val isPossibleDuplicate: Boolean = false,
    val duplicateReason: String? = null,
    val isNewCustomer: Boolean = false
)

data class AppSettingsState(
    val themePreset: AppThemePreset = AppThemePreset.PREMIUM_DARK,
    val customAccentColor: Long = 0xFFF59E0B,
    val customBackgroundStyle: CustomBackgroundStyle = CustomBackgroundStyle.MIDNIGHT_SLATE,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val businessName: String = "Desi Vyapar Kendra",
    val ownerMobile: String = "",
    val profilePhotoPath: String? = null,
    val notificationsEnabled: Boolean = true,
    val notifyOnTransaction: Boolean = true,
    val notifyPaymentReminder: Boolean = true,
    val notifyDailySummary: Boolean = true,
    val aiEngineMode: AiEngineMode = AiEngineMode.AUTO,
    val reminderTemplate: String = "Namaste {name}, aapka {business} par kul baki hisab ₹{balance} hai. Kripya samay par jama karein. Dhanyavaad!",
    val udharTemplate: String = "Namaste {name}, {business} khata mein ₹{amount} Udhar darj hua hai. Kul baki: ₹{balance}.",
    val jamaTemplate: String = "Namaste {name}, {business} khata mein ₹{amount} Jama prapt hua. Shesh baki: ₹{balance}. Dhanyavaad!",
    val summaryTemplate: String = "Hisab Summary ({business}): {name}, aapka current balance ₹{balance} hai. Date: {date}.",
    val scheduledDailySummaryEnabled: Boolean = false
)

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Thinking : AiUiState
    data class Error(val message: String) : AiUiState
}

fun formatInr(amount: Double, includeSymbol: Boolean = true): String {
    val symbols = DecimalFormatSymbols(Locale("en", "IN"))
    val pattern = if (abs(amount % 1.0) < 0.005) "#,##,##0" else "#,##,##0.00"
    val formatter = DecimalFormat(pattern, symbols)
    val absFormatted = formatter.format(abs(amount))
    val sign = if (amount < -0.004) "-" else ""
    return if (includeSymbol) "${sign}₹$absFormatted" else "$sign$absFormatted"
}
