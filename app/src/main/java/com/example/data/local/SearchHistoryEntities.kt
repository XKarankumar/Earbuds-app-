package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val id: String,
    val recordType: String,
    val encryptedPayload: String,
    val riskScore: Int,
    val createdAtMillis: Long
)
