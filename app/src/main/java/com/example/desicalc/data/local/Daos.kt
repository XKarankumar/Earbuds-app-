package com.example.desicalc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY updatedAt DESC, name ASC")
    fun observeAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE legacyExternalId = :legacyId LIMIT 1")
    suspend fun findByLegacyId(legacyId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findByExactName(name: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE mobile = :mobile AND mobile != '' LIMIT 1")
    suspend fun findByMobile(mobile: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC, id DESC")
    fun observeTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    suspend fun getTransactionsForCustomerOnce(customerId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE legacyExternalId = :legacyId LIMIT 1")
    suspend fun findByLegacyId(legacyId: String): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions
        WHERE customerId = :customerId
          AND type = :type
          AND ABS(amount - :amount) < 0.01
          AND timestamp >= :sinceTimestamp
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun findRecentDuplicate(
        customerId: Long,
        type: String,
        amount: Double,
        sinceTimestamp: Long
    ): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsForCustomer(customerId: Long)
}

@Dao
interface CalculationHistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC, id DESC LIMIT 200")
    fun observeHistory(): Flow<List<CalculationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CalculationHistoryEntity): Long

    @Query("DELETE FROM calculation_history")
    suspend fun clearAll()
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC, id DESC LIMIT 200")
    fun observeAllLogs(): Flow<List<SmsLogEntity>>

    @Query("SELECT * FROM sms_logs WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observeLogsForCustomer(customerId: Long): Flow<List<SmsLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SmsLogEntity): Long

    @Query("UPDATE sms_logs SET status = :status, errorDetail = :errorDetail WHERE id = :logId")
    suspend fun updateLogStatus(logId: Long, status: String, errorDetail: String?)
}

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeMessages(): Flow<List<AiChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatMessageEntity): Long

    @Query("UPDATE ai_chat_messages SET actionConfirmed = 1 WHERE id = :messageId")
    suspend fun markActionConfirmed(messageId: Long)

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearMessages()
}
