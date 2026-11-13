package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "owner_profile")
data class OwnerProfileEntity(
    @PrimaryKey val id: String = "owner",
    val encryptedPayload: String,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val encryptedPayload: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["customerId"]), Index(value = ["createdAtEpochMs"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val encryptedPayload: String,
    val integrityDigest: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["createdAtEpochMs"])]
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val encryptedPayload: String,
    val integrityDigest: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "calc_history",
    indices = [Index(value = ["createdAtEpochMs"])]
)
data class CalcHistoryEntity(
    @PrimaryKey val id: String,
    val encryptedPayload: String,
    val createdAtEpochMs: Long
)
