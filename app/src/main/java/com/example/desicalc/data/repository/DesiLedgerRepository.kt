package com.example.desicalc.data.repository

import androidx.room.withTransaction
import com.example.desicalc.data.local.AiChatDao
import com.example.desicalc.data.local.AiChatMessageEntity
import com.example.desicalc.data.local.CalculationHistoryDao
import com.example.desicalc.data.local.CalculationHistoryEntity
import com.example.desicalc.data.local.CustomerDao
import com.example.desicalc.data.local.CustomerEntity
import com.example.desicalc.data.local.DesiCalculatorDatabase
import com.example.desicalc.data.local.SmsLogDao
import com.example.desicalc.data.local.SmsLogEntity
import com.example.desicalc.data.local.TransactionDao
import com.example.desicalc.data.local.TransactionEntity
import com.example.desicalc.model.CustomerLedgerSummary
import com.example.desicalc.model.DailyChartBucket
import com.example.desicalc.model.DashboardMetrics
import com.example.desicalc.model.PeriodReport
import com.example.desicalc.model.ReportPeriod
import com.example.desicalc.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class DesiLedgerRepository(
    private val db: DesiCalculatorDatabase
) {
    private val customerDao: CustomerDao = db.customerDao()
    private val transactionDao: TransactionDao = db.transactionDao()
    private val calcDao: CalculationHistoryDao = db.calculationHistoryDao()
    private val smsLogDao: SmsLogDao = db.smsLogDao()
    private val aiChatDao: AiChatDao = db.aiChatDao()

    val customersFlow: Flow<List<CustomerEntity>> = customerDao.observeAllCustomers()
    val transactionsFlow: Flow<List<TransactionEntity>> = transactionDao.observeAllTransactions()
    val calculationHistoryFlow: Flow<List<CalculationHistoryEntity>> = calcDao.observeHistory()
    val smsLogsFlow: Flow<List<SmsLogEntity>> = smsLogDao.observeAllLogs()
    val aiChatFlow: Flow<List<AiChatMessageEntity>> = aiChatDao.observeMessages()

    val customerSummariesFlow: Flow<List<CustomerLedgerSummary>> = combine(
        customersFlow,
        transactionsFlow
    ) { customers, transactions ->
        val grouped = transactions.groupBy { it.customerId }
        customers.map { c ->
            buildCustomerSummary(c, grouped[c.id].orEmpty())
        }
    }

    val dashboardMetricsFlow: Flow<DashboardMetrics> = combine(
        customerSummariesFlow,
        transactionsFlow
    ) { summaries, transactions ->
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayTx = transactions.filter { it.timestamp >= todayStart }

        val totalUdharDiya = summaries.sumOf { it.udharDiyaTotal }
        val totalJamaReceived = summaries.sumOf { it.jamaReceivedTotal }
        val totalUdharLiya = summaries.sumOf { it.udharLiyaTotal }
        val totalPaymentMade = summaries.sumOf { it.paymentMadeTotal }
        val netReceivable = summaries.sumOf { maxOf(0.0, it.netBalance) }
        val netPayable = summaries.sumOf { maxOf(0.0, -it.netBalance) }

        val todayUdhar = todayTx.filter {
            val t = TransactionType.fromCode(it.type)
            t == TransactionType.UDHAR_DIYA || t == TransactionType.UDHAR_LIYA
        }.sumOf { it.amount }

        val todayJama = todayTx.filter {
            val t = TransactionType.fromCode(it.type)
            t == TransactionType.JAMA_RECEIVED || t == TransactionType.PAYMENT_MADE
        }.sumOf { it.amount }

        DashboardMetrics(
            totalCustomers = summaries.size,
            totalUdharDiya = totalUdharDiya,
            totalJamaReceived = totalJamaReceived,
            totalUdharLiya = totalUdharLiya,
            totalPaymentMade = totalPaymentMade,
            netReceivable = netReceivable,
            netPayable = netPayable,
            netRemainingBalance = netReceivable - netPayable,
            todayTransactionCount = todayTx.size,
            todayUdharTotal = todayUdhar,
            todayJamaTotal = todayJama,
            pendingCustomerCount = summaries.count { abs(it.netBalance) > 0.01 }
        )
    }

    fun observeTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeTransactionsForCustomer(customerId)

    suspend fun getCustomerById(id: Long): CustomerEntity? = customerDao.getCustomerById(id)

    suspend fun getAllSummariesOnce(): List<CustomerLedgerSummary> {
        val customers = customerDao.getAllCustomersOnce()
        val grouped = transactionDao.getAllTransactionsOnce().groupBy { it.customerId }
        return customers.map { c -> buildCustomerSummary(c, grouped[c.id].orEmpty()) }
    }

    suspend fun getAllTransactionsOnce(): List<TransactionEntity> =
        transactionDao.getAllTransactionsOnce()

    suspend fun addCustomer(
        name: String,
        mobile: String = "",
        address: String = "",
        notes: String = "",
        photoPath: String? = null,
        autoSmsEnabled: Boolean = false,
        smsConsentGranted: Boolean = false,
        reminderIntervalDays: Int = 0
    ): Long {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Customer name cannot be empty" }
        val existingCount = customerDao.getAllCustomersOnce().size + 1
        val code = "CUST-${1000 + existingCount}-${(System.currentTimeMillis() % 1000).toString().padStart(3, '0')}"
        val now = System.currentTimeMillis()
        return customerDao.insertCustomer(
            CustomerEntity(
                customerCode = code,
                name = trimmedName,
                mobile = mobile.trim(),
                address = address.trim(),
                notes = notes.trim(),
                photoPath = photoPath,
                autoSmsEnabled = autoSmsEnabled,
                smsConsentGranted = smsConsentGranted,
                reminderIntervalDays = reminderIntervalDays,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        require(customer.name.trim().isNotEmpty()) { "Customer name cannot be empty" }
        customerDao.updateCustomer(
            customer.copy(
                name = customer.name.trim(),
                mobile = customer.mobile.trim(),
                address = customer.address.trim(),
                notes = customer.notes.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCustomerSafely(customerId: Long, deleteTransactionsToo: Boolean = false): Boolean {
        val txCount = transactionDao.getTransactionsForCustomerOnce(customerId).size
        if (txCount > 0 && !deleteTransactionsToo) {
            return false
        }
        db.withTransaction {
            if (txCount > 0) {
                transactionDao.deleteTransactionsForCustomer(customerId)
            }
            customerDao.deleteCustomerById(customerId)
        }
        return true
    }

    suspend fun checkPossibleDuplicate(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        windowMinutes: Long = 15L
    ): TransactionEntity? {
        val since = System.currentTimeMillis() - windowMinutes * 60_000L
        return transactionDao.findRecentDuplicate(customerId, type.code, amount, since)
    }

    suspend fun addTransaction(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        description: String = "",
        isPartialPayment: Boolean = false,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        require(amount > 0.0 && amount.isFinite()) { "Amount must be greater than zero" }
        val customer = customerDao.getCustomerById(customerId)
            ?: throw IllegalArgumentException("Customer not found")

        return db.withTransaction {
            val id = transactionDao.insertTransaction(
                TransactionEntity(
                    customerId = customerId,
                    type = type.code,
                    amount = amount,
                    description = description.trim(),
                    isPartialPayment = isPartialPayment,
                    timestamp = timestamp,
                    updatedAt = System.currentTimeMillis()
                )
            )
            customerDao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
            id
        }
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        require(transaction.amount > 0.0 && transaction.amount.isFinite()) {
            "Amount must be greater than zero"
        }
        transactionDao.updateTransaction(
            transaction.copy(
                description = transaction.description.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteTransactionById(transactionId)
    }

    suspend fun addCalculationHistory(
        category: String,
        expression: String,
        result: String,
        details: String = ""
    ) {
        calcDao.insert(
            CalculationHistoryEntity(
                category = category,
                expression = expression,
                result = result,
                details = details
            )
        )
    }

    suspend fun clearCalculationHistory() {
        calcDao.clearAll()
    }

    suspend fun insertSmsLog(log: SmsLogEntity): Long = smsLogDao.insertLog(log)

    suspend fun updateSmsLogStatus(logId: Long, status: String, errorDetail: String? = null) {
        smsLogDao.updateLogStatus(logId, status, errorDetail)
    }

    suspend fun markCustomerReminderSent(customerId: Long) {
        val c = customerDao.getCustomerById(customerId) ?: return
        customerDao.updateCustomer(c.copy(lastReminderSentAt = System.currentTimeMillis()))
    }

    suspend fun insertAiMessage(message: AiChatMessageEntity): Long = aiChatDao.insertMessage(message)

    suspend fun markAiActionConfirmed(messageId: Long) = aiChatDao.markActionConfirmed(messageId)

    suspend fun clearAiChat() = aiChatDao.clearMessages()

    fun buildPeriodReport(
        transactions: List<TransactionEntity>,
        period: ReportPeriod
    ): PeriodReport {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startDate = when (period) {
            ReportPeriod.DAILY -> today
            ReportPeriod.WEEKLY -> today.minusDays(6)
            ReportPeriod.MONTHLY -> today.minusDays(29)
        }
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val filtered = transactions.filter { it.timestamp >= startMillis }

        var udharDiya = 0.0
        var jamaReceived = 0.0
        var udharLiya = 0.0
        var paymentMade = 0.0

        for (tx in filtered) {
            when (TransactionType.fromCode(tx.type)) {
                TransactionType.UDHAR_DIYA -> udharDiya += tx.amount
                TransactionType.JAMA_RECEIVED -> jamaReceived += tx.amount
                TransactionType.UDHAR_LIYA -> udharLiya += tx.amount
                TransactionType.PAYMENT_MADE -> paymentMade += tx.amount
            }
        }

        val fmt = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
        val buckets = when (period) {
            ReportPeriod.DAILY -> {
                listOf(
                    DailyChartBucket(
                        label = today.format(fmt),
                        udharAmount = udharDiya + udharLiya,
                        jamaAmount = jamaReceived + paymentMade
                    )
                )
            }
            ReportPeriod.WEEKLY -> {
                (0..6).map { offset ->
                    val d = startDate.plusDays(offset.toLong())
                    val dayStart = d.atStartOfDay(zone).toInstant().toEpochMilli()
                    val dayEnd = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val dayTx = filtered.filter { it.timestamp in dayStart until dayEnd }
                    DailyChartBucket(
                        label = d.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)),
                        udharAmount = dayTx.filter {
                            val t = TransactionType.fromCode(it.type)
                            t == TransactionType.UDHAR_DIYA || t == TransactionType.UDHAR_LIYA
                        }.sumOf { it.amount },
                        jamaAmount = dayTx.filter {
                            val t = TransactionType.fromCode(it.type)
                            t == TransactionType.JAMA_RECEIVED || t == TransactionType.PAYMENT_MADE
                        }.sumOf { it.amount }
                    )
                }
            }
            ReportPeriod.MONTHLY -> {
                // 5 buckets of 6 days each covering last 30 days
                (0..4).map { idx ->
                    val bStart = startDate.plusDays((idx * 6).toLong())
                    val bEnd = startDate.plusDays(((idx + 1) * 6).toLong())
                    val sMs = bStart.atStartOfDay(zone).toInstant().toEpochMilli()
                    val eMs = bEnd.atStartOfDay(zone).toInstant().toEpochMilli()
                    val bucketTx = filtered.filter { it.timestamp in sMs until eMs }
                    DailyChartBucket(
                        label = bStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)),
                        udharAmount = bucketTx.filter {
                            val t = TransactionType.fromCode(it.type)
                            t == TransactionType.UDHAR_DIYA || t == TransactionType.UDHAR_LIYA
                        }.sumOf { it.amount },
                        jamaAmount = bucketTx.filter {
                            val t = TransactionType.fromCode(it.type)
                            t == TransactionType.JAMA_RECEIVED || t == TransactionType.PAYMENT_MADE
                        }.sumOf { it.amount }
                    )
                }
            }
        }

        val periodTitle = when (period) {
            ReportPeriod.DAILY -> "Today (${today.format(fmt)})"
            ReportPeriod.WEEKLY -> "Last 7 Days (${startDate.format(fmt)} – ${today.format(fmt)})"
            ReportPeriod.MONTHLY -> "Last 30 Days (${startDate.format(fmt)} – ${today.format(fmt)})"
        }

        return PeriodReport(
            period = period,
            periodTitle = periodTitle,
            transactionCount = filtered.size,
            udharDiyaTotal = udharDiya,
            jamaReceivedTotal = jamaReceived,
            udharLiyaTotal = udharLiya,
            paymentMadeTotal = paymentMade,
            netFlow = (jamaReceived - udharDiya) + (udharLiya - paymentMade),
            dailyBuckets = buckets
        )
    }

    private fun buildCustomerSummary(
        c: CustomerEntity,
        txList: List<TransactionEntity>
    ): CustomerLedgerSummary {
        var udharDiya = 0.0
        var jamaReceived = 0.0
        var udharLiya = 0.0
        var paymentMade = 0.0
        var lastActivity = c.updatedAt

        for (tx in txList) {
            if (tx.timestamp > lastActivity) lastActivity = tx.timestamp
            when (TransactionType.fromCode(tx.type)) {
                TransactionType.UDHAR_DIYA -> udharDiya += tx.amount
                TransactionType.JAMA_RECEIVED -> jamaReceived += tx.amount
                TransactionType.UDHAR_LIYA -> udharLiya += tx.amount
                TransactionType.PAYMENT_MADE -> paymentMade += tx.amount
            }
        }

        return CustomerLedgerSummary(
            id = c.id,
            customerCode = c.customerCode,
            name = c.name,
            mobile = c.mobile,
            address = c.address,
            notes = c.notes,
            photoPath = c.photoPath,
            autoSmsEnabled = c.autoSmsEnabled,
            smsConsentGranted = c.smsConsentGranted,
            reminderIntervalDays = c.reminderIntervalDays,
            lastReminderSentAt = c.lastReminderSentAt,
            udharDiyaTotal = udharDiya,
            jamaReceivedTotal = jamaReceived,
            udharLiyaTotal = udharLiya,
            paymentMadeTotal = paymentMade,
            transactionCount = txList.size,
            lastActivityAt = lastActivity
        )
    }

    companion object {
        fun formatTimestamp(millis: Long): String {
            val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            return zdt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH))
        }
    }
}
