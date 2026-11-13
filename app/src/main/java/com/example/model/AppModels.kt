package com.example.model

enum class OceanEmeraldMode(val label: String, val isDark: Boolean) {
    DARK("Ocean Emerald Dark", true),
    LIGHT("Ocean Emerald Glass Light", false);

    companion object {
        fun fromKey(key: String?): OceanEmeraldMode =
            entries.firstOrNull { it.name == key } ?: DARK
    }
}

enum class AnimationPreset(val label: String, val description: String, val durationScale: Float) {
    FULL_SPRING("Full Spring", "Fluid spring physics & glowing aura motion", 1.0f),
    BALANCED("Balanced", "Crisp, fast transitions for daily speed", 0.6f),
    REDUCED_MOTION("Reduced Motion", "Minimal motion for battery & accessibility", 0.0f);

    companion object {
        fun fromKey(key: String?): AnimationPreset =
            entries.firstOrNull { it.name == key } ?: FULL_SPRING
    }
}

enum class CurrencyOption(val symbol: String, val code: String, val label: String) {
    INR("₹", "INR", "Indian Rupee (₹)"),
    USD("$", "USD", "US Dollar ($)"),
    EUR("€", "EUR", "Euro (€)"),
    GBP("£", "GBP", "British Pound (£)");

    companion object {
        fun fromKey(key: String?): CurrencyOption =
            entries.firstOrNull { it.name == key } ?: INR
    }
}

enum class TransactionType(val label: String) {
    JAMA("Jama (Received)"),
    UDHAAR("Udhaar (Given)");

    companion object {
        fun fromKey(key: String?): TransactionType =
            entries.firstOrNull { it.name == key } ?: UDHAAR
    }
}

enum class CalcRecordCategory(val label: String) {
    STANDARD("Calculator"),
    GST("GST"),
    EMI("EMI"),
    DISCOUNT("Discount"),
    INTEREST("Interest"),
    UNIT("Converter");

    companion object {
        fun fromKey(key: String?): CalcRecordCategory =
            entries.firstOrNull { it.name == key } ?: STANDARD
    }
}

sealed interface AppDestination {
    data object Calculator : AppDestination
    data object Dashboard : AppDestination
    data object HisabKitab : AppDestination
    data object SmsCenter : AppDestination
    data object ToolsHub : AppDestination
    data class CustomerProfile(val customerId: Long) : AppDestination
    data object History : AppDestination
    data object Reports : AppDestination
    data object UnitConverter : AppDestination
    data object GstCalculators : AppDestination
    data object Settings : AppDestination
}

data class CustomerSummary(
    val id: Long,
    val name: String,
    val phone: String,
    val notes: String,
    val createdAtMillis: Long,
    val totalJama: Double,
    val totalUdhaar: Double,
    val transactionCount: Int,
    val lastActivityMillis: Long
) {
    /** Positive = Customer owes you (Udhaar due); Negative = Advance Jama held */
    val netBalance: Double get() = totalUdhaar - totalJama
}

data class RecentTransactionItem(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String,
    val type: TransactionType,
    val amount: Double,
    val note: String,
    val expression: String,
    val createdAtMillis: Long
)

data class DashboardMetrics(
    val totalJama: Double = 0.0,
    val totalUdhaar: Double = 0.0,
    val customerCount: Int = 0,
    val transactionCount: Int = 0
) {
    val netBalance: Double get() = totalUdhaar - totalJama
}
