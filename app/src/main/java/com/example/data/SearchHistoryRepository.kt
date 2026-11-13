package com.example.data

import com.example.data.local.SearchHistoryDao
import com.example.data.local.SearchHistoryEntity
import com.example.model.HistoryRecordType
import com.example.model.SearchHistoryRecord
import com.example.security.CryptoVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class SearchHistoryRepository(private val dao: SearchHistoryDao) {

    val allRecords: Flow<List<SearchHistoryRecord>> = dao.observeAll().map { entities ->
        entities.mapNotNull { entity -> decodeEntity(entity) }
    }

    suspend fun saveRecord(
        recordType: HistoryRecordType,
        title: String,
        subtitle: String,
        riskLevel: String,
        riskScore: Int,
        summaryText: String,
        sourceAttribution: String
    ) = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val payloadJson = JSONObject().apply {
            put("title", title)
            put("subtitle", subtitle)
            put("riskLevel", riskLevel)
            put("summaryText", summaryText)
            put("sourceAttribution", sourceAttribution)
        }
        val encrypted = CryptoVault.encryptJson(payloadJson)
        dao.insert(
            SearchHistoryEntity(
                id = id,
                recordType = recordType.name,
                encryptedPayload = encrypted,
                riskScore = riskScore,
                createdAtMillis = now
            )
        )
    }

    suspend fun deleteRecord(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    private fun decodeEntity(entity: SearchHistoryEntity): SearchHistoryRecord? {
        val json = CryptoVault.decryptJson(entity.encryptedPayload) ?: return null
        val type = runCatching { HistoryRecordType.valueOf(entity.recordType) }
            .getOrDefault(HistoryRecordType.NUMBER)
        return SearchHistoryRecord(
            id = entity.id,
            recordType = type,
            title = json.optString("title", "Encrypted Record"),
            subtitle = json.optString("subtitle", ""),
            riskLevel = json.optString("riskLevel", "Low"),
            riskScore = entity.riskScore,
            summaryText = json.optString("summaryText", ""),
            sourceAttribution = json.optString("sourceAttribution", "Local Encrypted Vault"),
            createdAtMillis = entity.createdAtMillis
        )
    }
}
