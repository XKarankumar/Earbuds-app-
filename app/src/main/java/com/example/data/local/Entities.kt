package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberCode: String,
    val fullName: String,
    val countryCode: String,
    val encryptedPhone: String,
    val phoneLast4: String,
    val encryptedEmergencyName: String,
    val encryptedEmergencyPhone: String,
    val phoneVerified: Boolean = false,
    val photoPath: String? = null,
    val planType: String,
    val monthlyFee: Double,
    val joinDateMillis: Long,
    val expiryDateMillis: Long,
    val scheduledDaysCsv: String = "MON,TUE,WED,THU,FRI,SAT",
    val trainerId: Long? = null,
    val isActive: Boolean = true,
    val smsAttendanceMissing: Boolean = true,
    val smsWorkoutMissed: Boolean = true,
    val smsMembershipExpiry: Boolean = true,
    val smsFeePending: Boolean = true,
    val smsPaymentConfirm: Boolean = true,
    val smsWelcome: Boolean = true,
    val smsTokenExpiry: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "trainers")
data class TrainerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val specialty: String,
    val countryCode: String,
    val encryptedPhone: String,
    val shift: String,
    val monthlySalary: Double,
    val activeClients: Int = 0,
    val isActive: Boolean = true
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val memberName: String,
    val dateKey: String,
    val checkInMillis: Long,
    val status: String,
    val method: String
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val memberId: Long,
    val memberName: String,
    val amount: Double,
    val planType: String,
    val paymentMethod: String,
    val periodLabel: String,
    val paidAtMillis: Long,
    val isPaid: Boolean = true
)

@Entity(tableName = "workout_plan_items")
data class WorkoutPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: String,
    val routineName: String,
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val targetSets: Int,
    val targetReps: String,
    val restSeconds: Int,
    val lastCompletedDateKey: String = ""
)

@Entity(tableName = "custom_routines")
data class CustomRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val focusArea: String,
    val exerciseIdsCsv: String,
    val estimatedMinutes: Int,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_history")
data class WorkoutHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineName: String,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolumeKg: Double,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "diet_meals")
data class DietMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val mealSlot: String,
    val foodName: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int,
    val isLogged: Boolean = false
)

@Entity(tableName = "weight_logs")
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Float,
    val bodyFatPercent: Float = 0f,
    val note: String = "",
    val loggedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey val dateKey: String,
    val intakeMl: Int,
    val targetMl: Int
)

@Entity(tableName = "access_tokens")
data class AccessTokenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tokenHash: String,
    val maskedToken: String,
    val role: String,
    val label: String,
    val assignedMemberId: Long? = null,
    val deviceBindingHash: String? = null,
    val expiresAtMillis: Long,
    val isRevoked: Boolean = false,
    val lastVerifiedMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "token_audit")
data class TokenAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val maskedToken: String,
    val action: String,
    val role: String,
    val deviceHashShort: String,
    val success: Boolean,
    val detail: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "sms_logs")
data class SmsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long?,
    val memberName: String,
    val maskedPhone: String,
    val eventType: String,
    val messageBody: String,
    val deliveryStatus: String,
    val gatewayMode: String,
    val dedupeKey: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
