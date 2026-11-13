package com.example.model

enum class AppThemeId(val displayName: String, val subtitle: String, val isDark: Boolean) {
    OBSIDIAN_EMERALD("Obsidian Emerald", "Midnight glass & mint precision", true),
    ALABASTER_IVORY("Alabaster Ivory", "Crisp daylight executive paper", false),
    ROYAL_INDIGO("Royal Indigo", "Deep sapphire & periwinkle glow", true),
    SAFFRON_LEDGER("Saffron Ledger", "Warm heritage khata parchment", false),
    CYBER_SLATE("Cyber Slate", "High-contrast cyan terminal", true);

    companion object {
        fun fromKey(key: String?): AppThemeId =
            entries.firstOrNull { it.name == key } ?: OBSIDIAN_EMERALD
    }
}

enum class TransactionType(val label: String) {
    UDHAAR("Udhaar (Given)"),
    JAMA("Jama (Received)")
}

enum class SmsDispatchMethod {
    NONE,
    COMPOSER,
    DIRECT_SMS
}

enum class AuditAction(val label: String) {
    CUSTOMER_CREATED("Customer Added"),
    CUSTOMER_UPDATED("Customer Updated"),
    TX_UDHAAR("Udhaar Logged"),
    TX_JAMA("Jama Logged"),
    SMS_RECEIPT("SMS Receipt"),
    EXPORT_PDF("PDF Export"),
    EXPORT_CSV("CSV Export"),
    EXPORT_XLSX("Excel Export"),
    BACKUP_SYNC("Local Backup & Restore"),
    SECURITY_CHANGE("Security Update")
}

data class UserProfile(
    val id: String = "owner",
    val displayName: String = "Hardik Kumar",
    val businessName: String = "Hardik Kumar Enterprises",
    val phone: String = "",
    val preferredTheme: String = AppThemeId.OBSIDIAN_EMERALD.name,
    val lastBackupEpochMs: Long = 0L,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class Customer(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val notes: String = "",
    val totalUdhaarPaise: Long = 0L,
    val totalJamaPaise: Long = 0L,
    val balancePaise: Long = 0L,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

data class LedgerTransaction(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val type: String = TransactionType.UDHAAR.name,
    val amountPaise: Long = 0L,
    val note: String = "",
    val billReference: String = "",
    val smsReceiptSent: Boolean = false,
    val smsMethod: String = SmsDispatchMethod.NONE.name,
    val integrityDigest: String = "",
    val createdAtEpochMs: Long = 0L
)

data class AuditLog(
    val id: String = "",
    val action: String = AuditAction.BACKUP_SYNC.name,
    val entityId: String = "",
    val summary: String = "",
    val details: String = "",
    val integrityDigest: String = "",
    val createdAtEpochMs: Long = 0L
)

data class CalcHistoryEntry(
    val id: String = "",
    val expression: String = "",
    val result: String = "",
    val label: String = "",
    val createdAtEpochMs: Long = 0L
)

data class LocalBackupSnapshot(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val modifiedEpochMs: Long
)
