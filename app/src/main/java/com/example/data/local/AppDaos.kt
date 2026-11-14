package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members ORDER BY createdAtMillis DESC")
    suspend fun getAllOnce(): List<MemberEntity>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MemberEntity?

    @Query("SELECT * FROM members WHERE memberCode = :code LIMIT 1")
    suspend fun getByCode(code: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: MemberEntity): Long

    @Update
    suspend fun update(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM members")
    suspend fun count(): Int

    @Query("DELETE FROM members")
    suspend fun deleteAll()
}

@Dao
interface TrainerDao {
    @Query("SELECT * FROM trainers ORDER BY fullName ASC")
    fun observeAll(): Flow<List<TrainerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trainer: TrainerEntity): Long

    @Update
    suspend fun update(trainer: TrainerEntity)

    @Query("DELETE FROM trainers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM trainers")
    suspend fun count(): Int

    @Query("DELETE FROM trainers")
    suspend fun deleteAll()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY checkInMillis DESC")
    fun observeAll(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE dateKey = :dateKey ORDER BY checkInMillis DESC")
    fun observeByDate(dateKey: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE dateKey = :dateKey")
    suspend fun getByDateOnce(dateKey: String): List<AttendanceEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM attendance WHERE memberId = :memberId AND dateKey = :dateKey AND status != 'ABSENT')")
    suspend fun hasCheckedInOnDate(memberId: Long, dateKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AttendanceEntity): Long

    @Query("DELETE FROM attendance")
    suspend fun deleteAll()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paidAtMillis DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Query("UPDATE payments SET isPaid = :isPaid WHERE id = :id")
    suspend fun markPaid(id: Long, isPaid: Boolean)

    @Query("SELECT COUNT(*) FROM payments")
    suspend fun count(): Int

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_plan_items ORDER BY id ASC")
    fun observePlanItems(): Flow<List<WorkoutPlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItem(item: WorkoutPlanItemEntity): Long

    @Query("UPDATE workout_plan_items SET lastCompletedDateKey = :dateKey WHERE id = :id")
    suspend fun markPlanItemCompleted(id: Long, dateKey: String)

    @Query("DELETE FROM workout_plan_items WHERE id = :id")
    suspend fun deletePlanItem(id: Long)

    @Query("SELECT COUNT(*) FROM workout_plan_items")
    suspend fun planCount(): Int

    @Query("SELECT * FROM custom_routines ORDER BY createdAtMillis DESC")
    fun observeRoutines(): Flow<List<CustomRoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: CustomRoutineEntity): Long

    @Query("DELETE FROM custom_routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Query("SELECT * FROM workout_history ORDER BY completedAtMillis DESC")
    fun observeHistory(): Flow<List<WorkoutHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WorkoutHistoryEntity): Long

    @Query("DELETE FROM workout_plan_items")
    suspend fun deleteAllPlanItems()

    @Query("DELETE FROM custom_routines")
    suspend fun deleteAllRoutines()

    @Query("DELETE FROM workout_history")
    suspend fun deleteAllHistory()
}

@Dao
interface DietAndBioDao {
    @Query("SELECT * FROM diet_meals ORDER BY id DESC")
    fun observeAllMeals(): Flow<List<DietMealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: DietMealEntity): Long

    @Query("UPDATE diet_meals SET isLogged = :logged WHERE id = :id")
    suspend fun setMealLogged(id: Long, logged: Boolean)

    @Query("DELETE FROM diet_meals WHERE id = :id")
    suspend fun deleteMeal(id: Long)

    @Query("SELECT COUNT(*) FROM diet_meals")
    suspend fun mealCount(): Int

    @Query("SELECT * FROM weight_logs ORDER BY loggedAtMillis ASC")
    fun observeWeightLogs(): Flow<List<WeightLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLogEntity): Long

    @Query("SELECT COUNT(*) FROM weight_logs")
    suspend fun weightCount(): Int

    @Query("SELECT * FROM water_logs WHERE dateKey = :dateKey LIMIT 1")
    fun observeWaterForDate(dateKey: String): Flow<WaterLogEntity?>

    @Query("SELECT * FROM water_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getWaterForDate(dateKey: String): WaterLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWater(log: WaterLogEntity)

    @Query("DELETE FROM diet_meals")
    suspend fun deleteAllMeals()

    @Query("DELETE FROM weight_logs")
    suspend fun deleteAllWeights()

    @Query("DELETE FROM water_logs")
    suspend fun deleteAllWater()
}

@Dao
interface TokenDao {
    @Query("SELECT * FROM access_tokens ORDER BY createdAtMillis DESC")
    fun observeTokens(): Flow<List<AccessTokenEntity>>

    @Query("SELECT * FROM access_tokens WHERE tokenHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): AccessTokenEntity?

    @Query("SELECT COUNT(*) FROM access_tokens WHERE role = 'ADMIN' AND isRevoked = 0")
    suspend fun activeAdminTokenCount(): Int

    @Query("SELECT COUNT(*) FROM access_tokens")
    suspend fun totalTokenCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: AccessTokenEntity): Long

    @Update
    suspend fun updateToken(token: AccessTokenEntity)

    @Query("UPDATE access_tokens SET isRevoked = 1 WHERE id = :id")
    suspend fun revokeById(id: Long)

    @Query("UPDATE access_tokens SET expiresAtMillis = :newExpiry WHERE id = :id")
    suspend fun renewById(id: Long, newExpiry: Long)

    @Query("SELECT * FROM token_audit ORDER BY timestampMillis DESC LIMIT 100")
    fun observeAudits(): Flow<List<TokenAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: TokenAuditEntity): Long

    @Query("DELETE FROM access_tokens")
    suspend fun deleteAllTokens()

    @Query("DELETE FROM token_audit")
    suspend fun deleteAllAudits()
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY createdAtMillis DESC LIMIT 150")
    fun observeLogs(): Flow<List<SmsLogEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM sms_logs WHERE dedupeKey = :dedupeKey AND deliveryStatus != 'FAILED')")
    suspend fun existsByDedupeKey(dedupeKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SmsLogEntity): Long

    @Query("UPDATE sms_logs SET deliveryStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM sms_logs")
    suspend fun deleteAll()
}
