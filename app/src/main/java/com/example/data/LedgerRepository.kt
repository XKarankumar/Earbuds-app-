package com.example.data

import com.example.data.local.AuditLogEntity
import com.example.data.local.CalcHistoryEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerDao
import com.example.data.local.OwnerProfileEntity
import com.example.data.local.TransactionEntity
import com.example.model.AppThemeId
import com.example.model.AuditAction
import com.example.model.AuditLog
import com.example.model.CalcHistoryEntry
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.SmsDispatchMethod
import com.example.model.TransactionType
import com.example.model.UserProfile
import com.example.security.CryptoVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class LedgerRepository(private val dao: LedgerDao) {

    fun observeUserProfile(): Flow<UserProfile?> =
        dao.observeOwnerProfile().map { entity ->
            entity?.let { decodeOwnerProfile(it) }
        }

    fun observeCustomers(): Flow<List<Customer>> =
        dao.observeCustomers().map { rows ->
            rows.mapNotNull { decodeCustomer(it) }
        }

    fun observeTransactions(): Flow<List<LedgerTransaction>> =
        dao.observeTransactions().map { rows ->
            rows.mapNotNull { decodeTransaction(it) }
        }

    fun observeAuditLogs(): Flow<List<AuditLog>> =
        dao.observeAuditLogs().map { rows ->
            rows.mapNotNull { decodeAuditLog(it) }
        }

    fun observeCalcHistory(): Flow<List<CalcHistoryEntry>> =
        dao.observeCalcHistory().map { rows ->
            rows.mapNotNull { decodeCalcHistory(it) }
        }

    suspend fun ensureOwnerProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val existing = dao.getOwnerProfileOnce()?.let { decodeOwnerProfile(it) }
            if (existing != null) {
                Result.success(existing)
            } else {
                val now = System.currentTimeMillis()
                val profile = UserProfile(
                    id = "owner",
                    displayName = "Hardik Kumar",
                    businessName = "Hardik Kumar Enterprises",
                    phone = "",
                    preferredTheme = AppThemeId.OBSIDIAN_EMERALD.name,
                    lastBackupEpochMs = 0L,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
                dao.upsertOwnerProfile(encodeOwnerProfile(profile))
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOwnerProfileSettings(
        businessName: String? = null,
        phone: String? = null,
        preferredTheme: AppThemeId? = null,
        lastBackupEpochMs: Long? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val current = dao.getOwnerProfileOnce()?.let { decodeOwnerProfile(it) } ?: UserProfile(
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            val updated = current.copy(
                businessName = businessName?.trim()?.ifBlank { "Hardik Kumar Enterprises" }?.take(120)
                    ?: current.businessName,
                phone = phone?.trim()?.take(30) ?: current.phone,
                preferredTheme = preferredTheme?.name ?: current.preferredTheme,
                lastBackupEpochMs = lastBackupEpochMs ?: current.lastBackupEpochMs,
                updatedAtEpochMs = now
            )
            dao.upsertOwnerProfile(encodeOwnerProfile(updated))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCustomer(
        name: String,
        phone: String,
        notes: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanName = name.trim().take(100)
        require(cleanName.isNotEmpty()) { "Customer name is required" }
        try {
            val now = System.currentTimeMillis()
            val customerId = UUID.randomUUID().toString()
            val customer = Customer(
                id = customerId,
                name = cleanName,
                phone = phone.trim().take(30),
                notes = notes.trim().take(400),
                totalUdhaarPaise = 0L,
                totalJamaPaise = 0L,
                balancePaise = 0L,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            val audit = buildAuditEntity(
                action = AuditAction.CUSTOMER_CREATED,
                entityId = customerId,
                summary = "Added customer $cleanName".take(240),
                details = phone.trim().take(600),
                now = now
            )
            dao.createCustomerWithAudit(encodeCustomer(customer), audit)
            Result.success(customerId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerDetails(
        customerId: String,
        name: String,
        phone: String,
        notes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanName = name.trim().take(100)
        require(cleanName.isNotEmpty()) { "Customer name is required" }
        try {
            val existingEntity = dao.getCustomerById(customerId)
                ?: return@withContext Result.failure(IllegalStateException("Customer not found"))
            val existing = decodeCustomer(existingEntity)
                ?: return@withContext Result.failure(IllegalStateException("Failed to decrypt customer"))

            val now = System.currentTimeMillis()
            val updated = existing.copy(
                name = cleanName,
                phone = phone.trim().take(30),
                notes = notes.trim().take(400),
                updatedAtEpochMs = now
            )
            val audit = buildAuditEntity(
                action = AuditAction.CUSTOMER_UPDATED,
                entityId = customerId,
                summary = "Updated customer $cleanName".take(240),
                details = phone.trim().take(600),
                now = now
            )
            dao.updateCustomerWithAudit(encodeCustomer(updated), audit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordTransaction(
        customer: Customer,
        type: TransactionType,
        amountPaise: Long,
        note: String,
        billReference: String,
        smsReceiptSent: Boolean,
        smsMethod: SmsDispatchMethod
    ): Result<String> = withContext(Dispatchers.IO) {
        require(customer.id.isNotBlank()) { "Customer ID is required" }
        require(amountPaise > 0L) { "Amount must be positive" }
        try {
            val now = System.currentTimeMillis()
            val latest = dao.getCustomerById(customer.id)?.let { decodeCustomer(it) } ?: customer
            val newUdhaar = if (type == TransactionType.UDHAAR) {
                latest.totalUdhaarPaise + amountPaise
            } else {
                latest.totalUdhaarPaise
            }
            val newJama = if (type == TransactionType.JAMA) {
                latest.totalJamaPaise + amountPaise
            } else {
                latest.totalJamaPaise
            }
            val newBalance = newUdhaar - newJama

            val updatedCustomer = latest.copy(
                totalUdhaarPaise = newUdhaar,
                totalJamaPaise = newJama,
                balancePaise = newBalance,
                updatedAtEpochMs = now
            )

            val txId = UUID.randomUUID().toString()
            val tx = LedgerTransaction(
                id = txId,
                customerId = latest.id,
                customerName = latest.name.take(100),
                customerPhone = latest.phone.take(30),
                type = type.name,
                amountPaise = amountPaise,
                note = note.trim().take(400),
                billReference = billReference.trim().take(80),
                smsReceiptSent = smsReceiptSent,
                smsMethod = smsMethod.name,
                createdAtEpochMs = now
            )

            val formattedAmt = formatCurrencyPaise(amountPaise)
            val auditAction = if (type == TransactionType.UDHAAR) {
                AuditAction.TX_UDHAAR
            } else {
                AuditAction.TX_JAMA
            }
            val auditSummary = "${type.name}: $formattedAmt for ${latest.name}".take(240)
            val auditDetails = buildString {
                if (billReference.isNotBlank()) append("Ref: $billReference | ")
                if (note.isNotBlank()) append("Note: $note | ")
                append("SMS: ${smsMethod.name}")
            }.take(600)

            val audit = buildAuditEntity(
                action = auditAction,
                entityId = txId,
                summary = auditSummary,
                details = auditDetails,
                now = now
            )

            dao.recordTransactionAtomic(
                updatedCustomer = encodeCustomer(updatedCustomer),
                transaction = encodeTransaction(tx),
                audit = audit
            )
            Result.success(txId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logAuditEvent(
        action: AuditAction,
        entityId: String,
        summary: String,
        details: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val audit = buildAuditEntity(action, entityId, summary, details, now)
            dao.insertAuditLog(audit)
            Result.success(audit.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCalcHistory(
        expression: String,
        result: String,
        label: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val entry = CalcHistoryEntry(
                id = id,
                expression = expression.take(300),
                result = result.take(80),
                label = label.take(120),
                createdAtEpochMs = now
            )
            dao.insertCalcHistory(encodeCalcHistory(entry))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCalcHistory(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteCalcHistoryById(entryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreCustomersAndTransactions(
        customers: List<Customer>,
        transactions: List<LedgerTransaction>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var restoredCount = 0
            val now = System.currentTimeMillis()

            for (customer in customers) {
                val safeId = customer.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                if (dao.getCustomerById(safeId) == null) {
                    val safeUdhaar = customer.totalUdhaarPaise.coerceAtLeast(0L)
                    val safeJama = customer.totalJamaPaise.coerceAtLeast(0L)
                    val safeCustomer = customer.copy(
                        id = safeId,
                        name = customer.name.ifBlank { "Customer" }.take(100),
                        phone = customer.phone.take(30),
                        notes = customer.notes.take(400),
                        totalUdhaarPaise = safeUdhaar,
                        totalJamaPaise = safeJama,
                        balancePaise = safeUdhaar - safeJama,
                        createdAtEpochMs = if (customer.createdAtEpochMs > 0L) customer.createdAtEpochMs else now,
                        updatedAtEpochMs = now
                    )
                    dao.upsertCustomer(encodeCustomer(safeCustomer))
                    restoredCount++
                }
            }

            for (tx in transactions) {
                val safeId = tx.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                if (dao.getTransactionById(safeId) == null && tx.amountPaise > 0L && dao.getCustomerById(tx.customerId) != null) {
                    val safeTx = tx.copy(
                        id = safeId,
                        customerName = tx.customerName.ifBlank { "Customer" }.take(100),
                        customerPhone = tx.customerPhone.take(30),
                        type = if (tx.type == TransactionType.JAMA.name) TransactionType.JAMA.name else TransactionType.UDHAAR.name,
                        note = tx.note.take(400),
                        billReference = tx.billReference.take(80),
                        smsMethod = tx.smsMethod.ifBlank { SmsDispatchMethod.NONE.name },
                        createdAtEpochMs = if (tx.createdAtEpochMs > 0L) tx.createdAtEpochMs else now
                    )
                    dao.insertTransaction(encodeTransaction(safeTx))
                    restoredCount++
                }
            }

            dao.insertAuditLog(
                buildAuditEntity(
                    action = AuditAction.BACKUP_SYNC,
                    entityId = "local_vault",
                    summary = "Restored $restoredCount ledger records from local encrypted backup",
                    details = "Customers: ${customers.size}, Transactions: ${transactions.size}",
                    now = now
                )
            )
            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- AES-256-GCM Encrypted Serialization Helpers ---
    private fun encodeOwnerProfile(p: UserProfile): OwnerProfileEntity {
        val json = JSONObject().apply {
            put("displayName", p.displayName)
            put("businessName", p.businessName)
            put("phone", p.phone)
            put("preferredTheme", p.preferredTheme)
            put("lastBackupEpochMs", p.lastBackupEpochMs)
            put("createdAtEpochMs", p.createdAtEpochMs)
        }
        return OwnerProfileEntity(
            id = "owner",
            encryptedPayload = CryptoVault.encryptJson(json),
            updatedAtEpochMs = p.updatedAtEpochMs
        )
    }

    private fun decodeOwnerProfile(e: OwnerProfileEntity): UserProfile? {
        val json = CryptoVault.decryptJson(e.encryptedPayload) ?: return null
        return UserProfile(
            id = e.id,
            displayName = json.optString("displayName", "Hardik Kumar"),
            businessName = json.optString("businessName", "Hardik Kumar Enterprises"),
            phone = json.optString("phone"),
            preferredTheme = json.optString("preferredTheme", AppThemeId.OBSIDIAN_EMERALD.name),
            lastBackupEpochMs = json.optLong("lastBackupEpochMs", 0L),
            createdAtEpochMs = json.optLong("createdAtEpochMs", e.updatedAtEpochMs),
            updatedAtEpochMs = e.updatedAtEpochMs
        )
    }

    private fun encodeCustomer(c: Customer): CustomerEntity {
        val json = JSONObject().apply {
            put("name", c.name)
            put("phone", c.phone)
            put("notes", c.notes)
            put("totalUdhaarPaise", c.totalUdhaarPaise)
            put("totalJamaPaise", c.totalJamaPaise)
            put("balancePaise", c.balancePaise)
        }
        return CustomerEntity(
            id = c.id,
            encryptedPayload = CryptoVault.encryptJson(json),
            createdAtEpochMs = c.createdAtEpochMs,
            updatedAtEpochMs = c.updatedAtEpochMs
        )
    }

    private fun decodeCustomer(e: CustomerEntity): Customer? {
        val json = CryptoVault.decryptJson(e.encryptedPayload) ?: return null
        return Customer(
            id = e.id,
            name = json.optString("name"),
            phone = json.optString("phone"),
            notes = json.optString("notes"),
            totalUdhaarPaise = json.optLong("totalUdhaarPaise", 0L),
            totalJamaPaise = json.optLong("totalJamaPaise", 0L),
            balancePaise = json.optLong("balancePaise", 0L),
            createdAtEpochMs = e.createdAtEpochMs,
            updatedAtEpochMs = e.updatedAtEpochMs
        )
    }

    private fun encodeTransaction(tx: LedgerTransaction): TransactionEntity {
        val json = JSONObject().apply {
            put("customerName", tx.customerName)
            put("customerPhone", tx.customerPhone)
            put("type", tx.type)
            put("amountPaise", tx.amountPaise)
            put("note", tx.note)
            put("billReference", tx.billReference)
            put("smsReceiptSent", tx.smsReceiptSent)
            put("smsMethod", tx.smsMethod)
        }
        val digest = CryptoVault.sha256Hex(
            "${tx.id}|${tx.customerId}|${tx.type}|${tx.amountPaise}|${tx.createdAtEpochMs}"
        )
        return TransactionEntity(
            id = tx.id,
            customerId = tx.customerId,
            encryptedPayload = CryptoVault.encryptJson(json),
            integrityDigest = digest,
            createdAtEpochMs = tx.createdAtEpochMs
        )
    }

    private fun decodeTransaction(e: TransactionEntity): LedgerTransaction? {
        val json = CryptoVault.decryptJson(e.encryptedPayload) ?: return null
        return LedgerTransaction(
            id = e.id,
            customerId = e.customerId,
            customerName = json.optString("customerName"),
            customerPhone = json.optString("customerPhone"),
            type = json.optString("type", TransactionType.UDHAAR.name),
            amountPaise = json.optLong("amountPaise", 0L),
            note = json.optString("note"),
            billReference = json.optString("billReference"),
            smsReceiptSent = json.optBoolean("smsReceiptSent", false),
            smsMethod = json.optString("smsMethod", SmsDispatchMethod.NONE.name),
            integrityDigest = e.integrityDigest,
            createdAtEpochMs = e.createdAtEpochMs
        )
    }

    private fun buildAuditEntity(
        action: AuditAction,
        entityId: String,
        summary: String,
        details: String,
        now: Long
    ): AuditLogEntity {
        val id = UUID.randomUUID().toString()
        val safeSummary = summary.ifBlank { action.label }.take(240)
        val safeDetails = details.take(600)
        val json = JSONObject().apply {
            put("action", action.name)
            put("entityId", entityId.take(128))
            put("summary", safeSummary)
            put("details", safeDetails)
        }
        val digest = CryptoVault.sha256Hex("$id|${action.name}|$entityId|$safeSummary|$now")
        return AuditLogEntity(
            id = id,
            encryptedPayload = CryptoVault.encryptJson(json),
            integrityDigest = digest,
            createdAtEpochMs = now
        )
    }

    private fun decodeAuditLog(e: AuditLogEntity): AuditLog? {
        val json = CryptoVault.decryptJson(e.encryptedPayload) ?: return null
        return AuditLog(
            id = e.id,
            action = json.optString("action", AuditAction.BACKUP_SYNC.name),
            entityId = json.optString("entityId"),
            summary = json.optString("summary"),
            details = json.optString("details"),
            integrityDigest = e.integrityDigest,
            createdAtEpochMs = e.createdAtEpochMs
        )
    }

    private fun encodeCalcHistory(entry: CalcHistoryEntry): CalcHistoryEntity {
        val json = JSONObject().apply {
            put("expression", entry.expression)
            put("result", entry.result)
            put("label", entry.label)
        }
        return CalcHistoryEntity(
            id = entry.id,
            encryptedPayload = CryptoVault.encryptJson(json),
            createdAtEpochMs = entry.createdAtEpochMs
        )
    }

    private fun decodeCalcHistory(e: CalcHistoryEntity): CalcHistoryEntry? {
        val json = CryptoVault.decryptJson(e.encryptedPayload) ?: return null
        return CalcHistoryEntry(
            id = e.id,
            expression = json.optString("expression"),
            result = json.optString("result"),
            label = json.optString("label"),
            createdAtEpochMs = e.createdAtEpochMs
        )
    }

    companion object {
        fun formatCurrencyPaise(paise: Long): String {
            val rupees = paise / 100.0
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            return formatter.format(rupees)
        }
    }
}
