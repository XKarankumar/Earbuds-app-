package com.example.model

enum class ThemeMode(val label: String) {
    AMOLED_DARK("AMOLED Black"),
    LIGHT_GLASS("Light Glass");

    companion object {
        fun fromKey(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: AMOLED_DARK
    }
}

enum class UserRole(val label: String, val code: String) {
    ADMIN("Admin", "ADM"),
    TRAINER("Trainer", "TRN"),
    MEMBER("Member", "MEM");

    companion object {
        fun fromKey(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: MEMBER
    }
}

sealed interface AppDestination {
    data object Home : AppDestination
    data object Exercises : AppDestination
    data class ExerciseDetail(val exerciseId: String) : AppDestination
    data object Workout : AppDestination
    data object Diet : AppDestination
    data object Members : AppDestination
    data class MemberDetail(val memberId: Long) : AppDestination
    data object Attendance : AppDestination
    data object GymManagement : AppDestination
    data object TokenManagement : AppDestination
    data object Profile : AppDestination
    data object Settings : AppDestination
}

enum class MuscleGroup(val displayName: String, val shortLabel: String) {
    CHEST("Chest", "Chest"),
    BACK("Back", "Back"),
    SHOULDERS("Shoulders", "Delts"),
    BICEPS("Biceps", "Biceps"),
    TRICEPS("Triceps", "Triceps"),
    LEGS("Legs", "Legs"),
    ABS("Abs", "Abs"),
    FOREARMS("Forearms", "Forearms"),
    CALVES("Calves", "Calves"),
    CARDIO("Cardio", "Cardio")
}

enum class DifficultyLevel(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

enum class Exercise3DModelType {
    BENCH_PRESS,
    INCLINE_PRESS,
    PUSH_UP,
    CABLE_FLY,
    PULL_UP,
    LAT_PULLDOWN,
    BARBELL_ROW,
    DEADLIFT,
    OVERHEAD_PRESS,
    LATERAL_RAISE,
    FACE_PULL,
    BARBELL_CURL,
    HAMMER_CURL,
    PREACHER_CURL,
    TRICEP_PUSHDOWN,
    SKULL_CRUSHER,
    TRICEP_DIP,
    BARBELL_SQUAT,
    LEG_PRESS,
    LUNGES,
    ROMANIAN_DEADLIFT,
    HANGING_LEG_RAISE,
    CABLE_CRUNCH,
    PLANK,
    WRIST_CURL,
    FARMERS_WALK,
    REVERSE_CURL,
    STANDING_CALF_RAISE,
    SEATED_CALF_RAISE,
    JUMP_ROPE,
    TREADMILL_RUN,
    CYCLING_SPRINT,
    ROWING_MACHINE
}

data class ExerciseItem(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val difficulty: DifficultyLevel,
    val modelType: Exercise3DModelType,
    val postureGuide: String,
    val steps: List<String>,
    val defaultSets: Int,
    val defaultReps: String,
    val defaultRestSec: Int,
    val commonMistakes: List<String>,
    val safetyInstructions: List<String>,
    val caloriesPerSet: Int
)

data class CountryCode(
    val iso: String,
    val name: String,
    val dialCode: String,
    val minDigits: Int,
    val maxDigits: Int
) {
    companion object {
        val supported = listOf(
            CountryCode("IN", "India", "+91", 10, 10),
            CountryCode("US", "United States", "+1", 10, 10),
            CountryCode("UK", "United Kingdom", "+44", 10, 11),
            CountryCode("AE", "UAE", "+971", 9, 9),
            CountryCode("CA", "Canada", "+1", 10, 10),
            CountryCode("AU", "Australia", "+61", 9, 9),
            CountryCode("SG", "Singapore", "+65", 8, 8),
            CountryCode("DE", "Germany", "+49", 10, 11)
        )

        fun byDialCode(dial: String): CountryCode =
            supported.firstOrNull { it.dialCode == dial } ?: supported.first()
    }
}

enum class MembershipPlan(val label: String, val durationDays: Int, val defaultFeeInr: Double) {
    MONTHLY("Monthly", 30, 1500.0),
    QUARTERLY("Quarterly", 90, 4000.0),
    YEARLY("Yearly", 365, 14000.0);

    companion object {
        fun fromKey(raw: String?): MembershipPlan =
            entries.firstOrNull { it.name == raw } ?: MONTHLY
    }
}

enum class PaymentMethod(val label: String) {
    UPI("UPI"),
    CASH("Cash"),
    CARD("Card"),
    BANK("Bank Transfer")
}

enum class AttendanceStatus(val label: String) {
    PRESENT("Present"),
    LATE("Late"),
    ABSENT("Absent")
}

enum class SmsEventType(val label: String) {
    ATTENDANCE_MISSING("Attendance Missing"),
    WORKOUT_MISSED("Scheduled Workout Missed"),
    MEMBERSHIP_EXPIRY("Membership Expiry"),
    FEE_PENDING("Fee Pending"),
    PAYMENT_CONFIRMATION("Payment Confirmation"),
    NEW_MEMBER_REGISTRATION("New Member Registration"),
    TOKEN_EXPIRY("Token Expiry")
}

enum class SmsDeliveryStatus(val label: String) {
    QUEUED("Queued"),
    SENT_GATEWAY("Sent via HTTPS Gateway"),
    DELIVERED("Delivered"),
    HANDOFF_OPENED("SMS App Opened"),
    SKIPPED_DUPLICATE("Skipped (Duplicate Guard)"),
    SKIPPED_OPT_OUT("Skipped (Member Opt-Out)"),
    FAILED("Failed")
}

enum class FitnessGoalType(val label: String, val calorieDelta: Int, val proteinPerKg: Double) {
    FAT_LOSS("Fat Loss", -450, 2.0),
    MAINTENANCE("Maintenance", 0, 1.6),
    MUSCLE_GAIN("Muscle Gain", 350, 2.1),
    STRENGTH("Strength", 250, 2.0),
    ENDURANCE("Endurance", 200, 1.7);

    companion object {
        fun fromKey(raw: String?): FitnessGoalType =
            entries.firstOrNull { it.name == raw } ?: MUSCLE_GAIN
    }
}

enum class ActivityLevel(val label: String, val multiplier: Double) {
    SEDENTARY("Sedentary (Desk)", 1.2),
    LIGHT("Light (1–2 days/wk)", 1.375),
    MODERATE("Moderate (3–4 days/wk)", 1.55),
    ACTIVE("Active (5–6 days/wk)", 1.725),
    ATHLETE("Athlete (2x/day)", 1.9);

    companion object {
        fun fromKey(raw: String?): ActivityLevel =
            entries.firstOrNull { it.name == raw } ?: MODERATE
    }
}

enum class MealSlot(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    PRE_WORKOUT("Pre-Workout"),
    POST_WORKOUT("Post-Workout"),
    DINNER("Dinner")
}

data class MemberView(
    val id: Long,
    val memberCode: String,
    val fullName: String,
    val countryCode: String,
    val phoneNumber: String,
    val phoneLast4: String,
    val emergencyName: String,
    val emergencyPhone: String,
    val phoneVerified: Boolean,
    val photoPath: String?,
    val plan: MembershipPlan,
    val monthlyFee: Double,
    val joinDateMillis: Long,
    val expiryDateMillis: Long,
    val scheduledDays: Set<String>,
    val trainerId: Long?,
    val isActive: Boolean,
    val smsAttendanceMissing: Boolean,
    val smsWorkoutMissed: Boolean,
    val smsMembershipExpiry: Boolean,
    val smsFeePending: Boolean,
    val smsPaymentConfirm: Boolean,
    val smsWelcome: Boolean,
    val smsTokenExpiry: Boolean,
    val createdAtMillis: Long
)

data class TrainerView(
    val id: Long,
    val fullName: String,
    val specialty: String,
    val countryCode: String,
    val phoneNumber: String,
    val shift: String,
    val monthlySalary: Double,
    val activeClients: Int,
    val isActive: Boolean
)

data class TokenSessionState(
    val isAuthenticated: Boolean = false,
    val role: UserRole = UserRole.MEMBER,
    val maskedToken: String = "",
    val tokenLabel: String = "",
    val expiresAtMillis: Long = 0L,
    val offlineLeaseUntilMillis: Long = 0L,
    val isOfflineCached: Boolean = false,
    val deviceBound: Boolean = false,
    val hasAnyAdminToken: Boolean = false,
    val lockedOutUntilMillis: Long = 0L,
    val failedAttempts: Int = 0
)
