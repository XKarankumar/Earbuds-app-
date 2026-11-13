package com.example.data

import com.example.data.local.CalcHistoryDao
import com.example.data.local.CalcHistoryEntity
import com.example.data.local.CustomerDao
import com.example.data.local.CustomerEntity
import com.example.data.local.LedgerDao
import com.example.data.local.LedgerTransactionEntity
import com.example.data.local.SmsLogDao
import com.example.data.local.SmsLogEntity
import com.example.model.CalcRecordCategory
import com.example.model.CustomerSummary
import com.example.model.DashboardMetrics
import com.example.model.RecentTransactionItem
import com.example.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AppRepository(
    private val customerDao: CustomerDao,
    private val ledgerDao: LedgerDao,
    private val calcHistoryDao: CalcHistoryDao,
    private val smsLogDao: SmsLogDao
) {
    val customersFlow: Flow<List<CustomerEntity>> = customerDao.observeAll()
    val allTransactionsFlow: Flow<List<LedgerTransactionEntity>> = ledgerDao.observeAll()
    val calcHistoryFlow: Flow<List<CalcHistoryEntity>> = calcHistoryDao.observeAll()
    val smsLogsFlow: Flow<List<SmsLogEntity>> = smsLogDao.observeAll()

    val customerSummariesFlow: Flow<List<CustomerSummary>> = combine(
        customersFlow,
        allTransactionsFlow
    ) { customers, transactions ->
        val txByCustomer = transactions.groupBy { it.customerId }
        customers.map { c ->
            val list = txByCustomer[c.id].orEmpty()
            var jama = 0.0
            var udhaar = 0.0
            var lastAt = c.createdAtMillis
            list.forEach { tx ->
                when (TransactionType.fromKey(tx.type)) {
                    TransactionType.JAMA -> jama += tx.amount
                    TransactionType.UDHAAR -> udhaar += tx.amount
                }
                if (tx.createdAtMillis > lastAt) lastAt = tx.createdAtMillis
            }
            CustomerSummary(
                id = c.id,
                name = c.name,
                phone = c.phone,
                notes = c.notes,
                createdAtMillis = c.createdAtMillis,
                totalJama = jama,
                totalUdhaar = udhaar,
                transactionCount = list.size,
                lastActivityMillis = lastAt
            )
        }.sortedByDescending { it.lastActivityMillis }
    }

    val recentTransactionsFlow: Flow<List<RecentTransactionItem>> = combine(
        customersFlow,
        allTransactionsFlow
    ) { customers, transactions ->
        val customerMap = customers.associateBy { it.id }
        transactions.mapNotNull { tx ->
            val c = customerMap[tx.customerId] ?: return@mapNotNull null
            RecentTransactionItem(
                id = tx.id,
                customerId = c.id,
                customerName = c.name,
                customerPhone = c.phone,
                type = TransactionType.fromKey(tx.type),
                amount = tx.amount,
                note = tx.note,
                expression = tx.expression,
                createdAtMillis = tx.createdAtMillis
            )
        }
    }

    val dashboardMetricsFlow: Flow<DashboardMetrics> = combine(
        customersFlow,
        allTransactionsFlow
    ) { customers, transactions ->
        var jama = 0.0
        var udhaar = 0.0
        transactions.forEach { tx ->
            when (TransactionType.fromKey(tx.type)) {
                TransactionType.JAMA -> jama += tx.amount
                TransactionType.UDHAAR -> udhaar += tx.amount
            }
        }
        DashboardMetrics(
            totalJama = jama,
            totalUdhaar = udhaar,
            customerCount = customers.size,
            transactionCount = transactions.size
        )
    }

    fun observeCustomerTransactions(customerId: Long): Flow<List<LedgerTransactionEntity>> =
        ledgerDao.observeForCustomer(customerId)

    suspend fun addCustomer(
        name: String,
        phone: String,
        notes: String = "",
        openingAmount: Double = 0.0,
        openingType: TransactionType = TransactionType.UDHAAR
    ): Long {
        val id = customerDao.insert(
            CustomerEntity(
                name = name.trim(),
                phone = phone.trim(),
                notes = notes.trim()
            )
        )
        if (openingAmount > 0.0) {
            ledgerDao.insert(
                LedgerTransactionEntity(
                    customerId = id,
                    type = openingType.name,
                    amount = openingAmount,
                    note = "Opening balance"
                )
            )
        }
        return id
    }

    suspend fun updateCustomer(customer: CustomerEntity) {
        customerDao.update(customer)
    }

    suspend fun deleteCustomer(customerId: Long) {
        customerDao.deleteById(customerId)
    }

    suspend fun addTransaction(
        customerId: Long,
        type: TransactionType,
        amount: Double,
        note: String = "",
        expression: String = ""
    ): Long {
        return ledgerDao.insert(
            LedgerTransactionEntity(
                customerId = customerId,
                type = type.name,
                amount = amount.coerceAtLeast(0.0),
                note = note.trim(),
                expression = expression.trim()
            )
        )
    }

    suspend fun deleteTransaction(transactionId: Long) {
        ledgerDao.deleteById(transactionId)
    }

    suspend fun saveCalculation(
        category: CalcRecordCategory,
        expression: String,
        result: String
    ) {
        if (expression.isBlank() || result.isBlank()) return
        calcHistoryDao.insert(
            CalcHistoryEntity(
                category = category.name,
                expression = expression,
                result = result
            )
        )
    }

    suspend fun deleteCalculation(id: Long) {
        calcHistoryDao.deleteById(id)
    }

    suspend fun clearCalculationHistory() {
        calcHistoryDao.clearAll()
    }

    suspend fun saveSmsLog(
        recipientName: String,
        recipientPhone: String,
        message: String,
        templateName: String
    ) {
        smsLogDao.insert(
            SmsLogEntity(
                recipientName = recipientName.ifBlank { "Recipient" },
                recipientPhone = recipientPhone,
                message = message,
                templateName = templateName
            )
        )
    }

    suspend fun deleteSmsLog(id: Long) {
        smsLogDao.deleteById(id)
    }

    suspend fun clearSmsLogs() {
        smsLogDao.clearAll()
    }
}
