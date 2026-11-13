package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    // --- Owner Profile ---
    @Query("SELECT * FROM owner_profile WHERE id = 'owner' LIMIT 1")
    fun observeOwnerProfile(): Flow<OwnerProfileEntity?>

    @Query("SELECT * FROM owner_profile WHERE id = 'owner' LIMIT 1")
    suspend fun getOwnerProfileOnce(): OwnerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOwnerProfile(entity: OwnerProfileEntity)

    // --- Customers ---
    @Query("SELECT * FROM customers ORDER BY updatedAtEpochMs DESC, createdAtEpochMs DESC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(entity: CustomerEntity)

    @Transaction
    suspend fun createCustomerWithAudit(customer: CustomerEntity, audit: AuditLogEntity) {
        upsertCustomer(customer)
        insertAuditLog(audit)
    }

    @Transaction
    suspend fun updateCustomerWithAudit(customer: CustomerEntity, audit: AuditLogEntity) {
        upsertCustomer(customer)
        insertAuditLog(audit)
    }

    // --- Transactions (Strictly Append-Only) ---
    @Query("SELECT * FROM transactions ORDER BY createdAtEpochMs DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(entity: TransactionEntity)

    @Transaction
    suspend fun recordTransactionAtomic(
        updatedCustomer: CustomerEntity,
        transaction: TransactionEntity,
        audit: AuditLogEntity
    ) {
        upsertCustomer(updatedCustomer)
        insertTransaction(transaction)
        insertAuditLog(audit)
    }

    // --- Audit Logs (Strictly Append-Only) ---
    @Query("SELECT * FROM audit_logs ORDER BY createdAtEpochMs DESC")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAuditLog(entity: AuditLogEntity)

    // --- Calculator Tape History ---
    @Query("SELECT * FROM calc_history ORDER BY createdAtEpochMs DESC")
    fun observeCalcHistory(): Flow<List<CalcHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalcHistory(entity: CalcHistoryEntity)

    @Query("DELETE FROM calc_history WHERE id = :id")
    suspend fun deleteCalcHistoryById(id: String)
}
