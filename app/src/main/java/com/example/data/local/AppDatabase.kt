package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MemberEntity::class,
        TrainerEntity::class,
        AttendanceEntity::class,
        PaymentEntity::class,
        WorkoutPlanItemEntity::class,
        CustomRoutineEntity::class,
        WorkoutHistoryEntity::class,
        DietMealEntity::class,
        WeightLogEntity::class,
        WaterLogEntity::class,
        AccessTokenEntity::class,
        TokenAuditEntity::class,
        SmsLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun trainerDao(): TrainerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun dietAndBioDao(): DietAndBioDao
    abstract fun tokenDao(): TokenDao
    abstract fun smsLogDao(): SmsLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_hub_v1.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
