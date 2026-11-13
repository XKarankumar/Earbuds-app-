package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteById(customerId: Long)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_transactions ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE customerId = :customerId ORDER BY createdAtMillis DESC")
    fun observeForCustomer(customerId: Long): Flow<List<LedgerTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: LedgerTransactionEntity): Long

    @Query("DELETE FROM ledger_transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long)
}

@Dao
interface CalcHistoryDao {
    @Query("SELECT * FROM calc_history ORDER BY createdAtMillis DESC LIMIT 150")
    fun observeAll(): Flow<List<CalcHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CalcHistoryEntity): Long

    @Query("DELETE FROM calc_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calc_history")
    suspend fun clearAll()
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY createdAtMillis DESC LIMIT 100")
    fun observeAll(): Flow<List<SmsLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SmsLogEntity): Long

    @Query("DELETE FROM sms_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAll()
}
