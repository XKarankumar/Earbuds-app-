package com.example.desicalc.data.backup

import androidx.room.withTransaction
import com.example.desicalc.data.local.CustomerEntity
import com.example.desicalc.data.local.DesiCalculatorDatabase
import com.example.desicalc.data.local.TransactionEntity
import com.example.desicalc.model.TransactionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class DesiBackupV2(
    val formatVersion: Int = 2,
    val app: String = "Desi Calculator AI PRO V2.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val customers: List<BackupCustomer> = emptyList(),
    val transactions: List<BackupTransaction> = emptyList()
)

@Serializable
data class BackupCustomer(
    val legacyId: String,
    val customerCode: String,
    val name: String,
    val mobile: String = "",
    val address: String = "",
    val notes: String = "",
    val autoSmsEnabled: Boolean = false,
    val smsConsentGranted: Boolean = false,
    val reminderIntervalDays: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class BackupTransaction(
    val legacyId: String,
    val customerLegacyId: String,
    val customerName: String = "",
    val type: String,
    val amount: Double,
    val description: String = "",
    val isPartialPayment: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class MigrationImportResult(
    val customersImported: Int,
    val customersSkippedExisting: Int,
    val transactionsImported: Int,
    val transactionsSkippedExisting: Int,
    val sourceFormatDetected: String
)

class BackupAndMigrationManager(
    private val db: DesiCalculatorDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportFullJsonBackup(): String {
        val customers = db.customerDao().getAllCustomersOnce()
        val transactions = db.transactionDao().getAllTransactionsOnce()
        val customerById = customers.associateBy { it.id }

        val backup = DesiBackupV2(
            customers = customers.map { c ->
                BackupCustomer(
                    legacyId = c.legacyExternalId ?: "v2_c_${c.id}",
                    customerCode = c.customerCode,
                    name = c.name,
                    mobile = c.mobile,
                    address = c.address,
                    notes = c.notes,
                    autoSmsEnabled = c.autoSmsEnabled,
                    smsConsentGranted = c.smsConsentGranted,
                    reminderIntervalDays = c.reminderIntervalDays,
                    createdAt = c.createdAt
                )
            },
            transactions = transactions.map { t ->
                val cust = customerById[t.customerId]
                BackupTransaction(
                    legacyId = t.legacyExternalId ?: "v2_t_${t.id}",
                    customerLegacyId = cust?.legacyExternalId ?: "v2_c_${t.customerId}",
                    customerName = cust?.name.orEmpty(),
                    type = t.type,
                    amount = t.amount,
                    description = t.description,
                    isPartialPayment = t.isPartialPayment,
                    timestamp = t.timestamp
                )
            }
        )
        return json.encodeToString(DesiBackupV2.serializer(), backup)
    }

    suspend fun exportLedgerCsv(): String {
        val customers = db.customerDao().getAllCustomersOnce()
        val transactions = db.transactionDao().getAllTransactionsOnce()
        val customerMap = customers.associateBy { it.id }
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

        val sb = StringBuilder()
        sb.appendLine("TransactionID,Date,CustomerCode,CustomerName,Mobile,Type,AmountINR,PartialPayment,Description")
        for (tx in transactions) {
            val c = customerMap[tx.customerId]
            val dateStr = Instant.ofEpochMilli(tx.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(fmt)
            sb.appendLine(
                listOf(
                    tx.id.toString(),
                    csvEscape(dateStr),
                    csvEscape(c?.customerCode ?: ""),
                    csvEscape(c?.name ?: "Unknown"),
                    csvEscape(c?.mobile ?: ""),
                    csvEscape(tx.type),
                    String.format(Locale.US, "%.2f", tx.amount),
                    tx.isPartialPayment.toString(),
                    csvEscape(tx.description)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    /**
     * Non-destructive restore & legacy HTML LocalStorage migration:
     * Supports:
     * 1) Desi Calculator AI PRO V2.0 JSON backups
     * 2) Legacy `custom_hisab_calculator.html` LocalStorage exports (customers/transactions/entries/hisabData)
     * Never deletes or overwrites existing records!
     */
    suspend fun importJsonNonDestructive(rawJson: String): MigrationImportResult {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Backup JSON is empty" }

        val parsedCustomers = mutableListOf<BackupCustomer>()
        val parsedTransactions = mutableListOf<BackupTransaction>()
        var formatLabel = "Desi Calculator V2 Backup"

        val root = JSONObject(trimmed)
        if (root.optInt("formatVersion", 0) >= 2 && root.has("customers")) {
            val decoded = json.decodeFromString(DesiBackupV2.serializer(), trimmed)
            parsedCustomers += decoded.customers
            parsedTransactions += decoded.transactions
        } else {
            formatLabel = "Legacy HTML LocalStorage Export"
            parseLegacyHtmlJson(root, parsedCustomers, parsedTransactions)
        }

        var cImported = 0
        var cSkipped = 0
        var tImported = 0
        var tSkipped = 0

        db.withTransaction {
            val customerDao = db.customerDao()
            val txDao = db.transactionDao()
            val legacyToRoomId = mutableMapOf<String, Long>()
            val nameToRoomId = mutableMapOf<String, Long>()

            for (existing in customerDao.getAllCustomersOnce()) {
                existing.legacyExternalId?.let { legacyToRoomId[it] = existing.id }
                nameToRoomId[existing.name.lowercase(Locale.ROOT).trim()] = existing.id
            }

            for ((idx, bc) in parsedCustomers.withIndex()) {
                val cleanName = bc.name.trim()
                if (cleanName.isEmpty()) continue

                val byLegacy = customerDao.findByLegacyId(bc.legacyId)
                val byMobile = if (bc.mobile.isNotBlank()) customerDao.findByMobile(bc.mobile.trim()) else null
                val byName = customerDao.findByExactName(cleanName)
                val matched = byLegacy ?: byMobile ?: byName

                if (matched != null) {
                    legacyToRoomId[bc.legacyId] = matched.id
                    nameToRoomId[cleanName.lowercase(Locale.ROOT)] = matched.id
                    cSkipped++
                } else {
                    val code = bc.customerCode.ifBlank {
                        "MIG-${1000 + idx + customerDao.getAllCustomersOnce().size}"
                    }
                    val newId = customerDao.insertCustomer(
                        CustomerEntity(
                            customerCode = ensureUniqueCode(code, customerDao.getAllCustomersOnce().map { it.customerCode }.toSet()),
                            name = cleanName,
                            mobile = bc.mobile.trim(),
                            address = bc.address.trim(),
                            notes = bc.notes.trim(),
                            autoSmsEnabled = bc.autoSmsEnabled,
                            smsConsentGranted = bc.smsConsentGranted,
                            reminderIntervalDays = bc.reminderIntervalDays,
                            legacyExternalId = bc.legacyId,
                            createdAt = bc.createdAt,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    legacyToRoomId[bc.legacyId] = newId
                    nameToRoomId[cleanName.lowercase(Locale.ROOT)] = newId
                    cImported++
                }
            }

            for ((idx, bt) in parsedTransactions.withIndex()) {
                if (bt.amount <= 0.0 || !bt.amount.isFinite()) continue
                val existingTx = txDao.findByLegacyId(bt.legacyId)
                if (existingTx != null) {
                    tSkipped++
                    continue
                }

                var targetCustomerId = legacyToRoomId[bt.customerLegacyId]
                    ?: nameToRoomId[bt.customerName.lowercase(Locale.ROOT).trim()]

                if (targetCustomerId == null && bt.customerName.isNotBlank()) {
                    val clean = bt.customerName.trim()
                    val code = "MIG-C-${System.currentTimeMillis() % 10000}-$idx"
                    targetCustomerId = customerDao.insertCustomer(
                        CustomerEntity(
                            customerCode = code,
                            name = clean,
                            legacyExternalId = bt.customerLegacyId.ifBlank { "legacy_name_$clean" }
                        )
                    )
                    nameToRoomId[clean.lowercase(Locale.ROOT)] = targetCustomerId
                    cImported++
                }

                if (targetCustomerId != null) {
                    val txType = TransactionType.fromCode(bt.type)
                    // Also check duplicate by exact timestamp + amount + customerId + type
                    val sameCustomerTx = txDao.getTransactionsForCustomerOnce(targetCustomerId)
                    val exactDup = sameCustomerTx.any {
                        it.type == txType.code &&
                            kotlin.math.abs(it.amount - bt.amount) < 0.01 &&
                            kotlin.math.abs(it.timestamp - bt.timestamp) < 1000L
                    }
                    if (exactDup) {
                        tSkipped++
                    } else {
                        txDao.insertTransaction(
                            TransactionEntity(
                                customerId = targetCustomerId,
                                type = txType.code,
                                amount = bt.amount,
                                description = bt.description.trim(),
                                isPartialPayment = bt.isPartialPayment,
                                timestamp = bt.timestamp,
                                legacyExternalId = bt.legacyId
                            )
                        )
                        tImported++
                    }
                }
            }
        }

        return MigrationImportResult(
            customersImported = cImported,
            customersSkippedExisting = cSkipped,
            transactionsImported = tImported,
            transactionsSkippedExisting = tSkipped,
            sourceFormatDetected = formatLabel
        )
    }

    private fun parseLegacyHtmlJson(
        root: JSONObject,
        customersOut: MutableList<BackupCustomer>,
        txOut: MutableList<BackupTransaction>
    ) {
        val custArray: JSONArray? = root.optJSONArray("customers")
            ?: root.optJSONArray("hisab_customers")
            ?: root.optJSONArray("accounts")

        if (custArray != null) {
            for (i in 0 until custArray.length()) {
                val obj = custArray.optJSONObject(i) ?: continue
                val id = obj.optString("id", obj.optString("customerId", "legacy_c_$i"))
                val name = obj.optString("name", obj.optString("customerName", "")).trim()
                if (name.isEmpty()) continue
                val mobile = obj.optString("mobile", obj.optString("phone", "")).trim()
                val address = obj.optString("address", "").trim()
                val notes = obj.optString("notes", obj.optString("note", "")).trim()
                val createdAt = parseFlexibleTimestamp(obj.opt("createdAt") ?: obj.opt("date"))

                customersOut += BackupCustomer(
                    legacyId = id,
                    customerCode = obj.optString("customerCode", ""),
                    name = name,
                    mobile = mobile,
                    address = address,
                    notes = notes,
                    createdAt = createdAt
                )

                // Support nested transactions inside customer object
                val nestedTx = obj.optJSONArray("transactions") ?: obj.optJSONArray("entries")
                if (nestedTx != null) {
                    for (j in 0 until nestedTx.length()) {
                        val tObj = nestedTx.optJSONObject(j) ?: continue
                        val amount = tObj.optDouble("amount", 0.0)
                        if (amount > 0.0) {
                            txOut += BackupTransaction(
                                legacyId = tObj.optString("id", "legacy_nt_${i}_$j"),
                                customerLegacyId = id,
                                customerName = name,
                                type = tObj.optString("type", tObj.optString("kind", "udhar")),
                                amount = amount,
                                description = tObj.optString("description", tObj.optString("note", "")),
                                timestamp = parseFlexibleTimestamp(tObj.opt("timestamp") ?: tObj.opt("date"))
                            )
                        }
                    }
                }
            }
        }

        val txArray: JSONArray? = root.optJSONArray("transactions")
            ?: root.optJSONArray("udhar_jama_entries")
            ?: root.optJSONArray("entries")
            ?: root.optJSONArray("hisabData")

        if (txArray != null) {
            for (i in 0 until txArray.length()) {
                val tObj = txArray.optJSONObject(i) ?: continue
                val amount = tObj.optDouble("amount", 0.0)
                if (amount <= 0.0) continue
                val custId = tObj.optString("customerId", tObj.optString("customerLegacyId", ""))
                val custName = tObj.optString("customerName", tObj.optString("name", ""))
                txOut += BackupTransaction(
                    legacyId = tObj.optString("id", "legacy_t_$i"),
                    customerLegacyId = custId,
                    customerName = custName,
                    type = tObj.optString("type", tObj.optString("kind", "udhar")),
                    amount = amount,
                    description = tObj.optString("description", tObj.optString("note", "")),
                    timestamp = parseFlexibleTimestamp(tObj.opt("timestamp") ?: tObj.opt("date"))
                )
            }
        }
    }

    private fun parseFlexibleTimestamp(value: Any?): Long {
        return when (value) {
            is Number -> {
                val l = value.toLong()
                if (l in 1..9999999999L) l * 1000L else l
            }
            is String -> {
                value.toLongOrNull() ?: runCatching {
                    Instant.parse(value).toEpochMilli()
                }.getOrDefault(System.currentTimeMillis())
            }
            else -> System.currentTimeMillis()
        }
    }

    private fun ensureUniqueCode(candidate: String, existingCodes: Set<String>): String {
        if (candidate !in existingCodes) return candidate
        var n = 1
        while ("$candidate-$n" in existingCodes) n++
        return "$candidate-$n"
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }
}
