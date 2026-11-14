package com.example.domain

import com.example.model.ActivityLevel
import com.example.model.CountryCode
import com.example.model.FitnessGoalType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

data class BmiResult(
    val bmi: Float,
    val category: String,
    val healthyMinKg: Float,
    val healthyMaxKg: Float,
    val normalizedGauge: Float
)

data class NutritionTargets(
    val bmr: Int,
    val tdee: Int,
    val targetCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int
)

object FitnessCalculators {

    fun calculateBmi(weightKg: Float, heightCm: Float): BmiResult {
        val hM = (heightCm / 100f).coerceIn(1.0f, 2.5f)
        val w = weightKg.coerceIn(25f, 300f)
        val bmi = (w / hM.pow(2))
        val rounded = (bmi * 10f).roundToInt() / 10f
        val category = when {
            rounded < 18.5f -> "Underweight"
            rounded < 25.0f -> "Normal (Healthy)"
            rounded < 30.0f -> "Overweight"
            else -> "Obese"
        }
        val minKg = ((18.5f * hM.pow(2)) * 10f).roundToInt() / 10f
        val maxKg = ((24.9f * hM.pow(2)) * 10f).roundToInt() / 10f
        val gauge = ((rounded - 14f) / (38f - 14f)).coerceIn(0f, 1f)
        return BmiResult(
            bmi = rounded,
            category = category,
            healthyMinKg = minKg,
            healthyMaxKg = maxKg,
            normalizedGauge = gauge
        )
    }

    /**
     * Mifflin-St Jeor BMR + TDEE + Goal Macro Split
     */
    fun calculateNutritionTargets(
        age: Int,
        isMale: Boolean,
        weightKg: Float,
        heightCm: Float,
        activityLevel: ActivityLevel,
        goalType: FitnessGoalType
    ): NutritionTargets {
        val sexOffset = if (isMale) 5.0 else -161.0
        val bmr = (10.0 * weightKg + 6.25 * heightCm - 5.0 * age + sexOffset).roundToInt().coerceAtLeast(1100)
        val tdee = (bmr * activityLevel.multiplier).roundToInt()
        val targetCal = (tdee + goalType.calorieDelta).coerceIn(1200, 5500)
        val proteinG = (weightKg * goalType.proteinPerKg).roundToInt().coerceIn(50, 350)
        val fatsG = ((targetCal * 0.25) / 9.0).roundToInt().coerceIn(30, 180)
        val remainingCal = (targetCal - (proteinG * 4 + fatsG * 9)).coerceAtLeast(200)
        val carbsG = (remainingCal / 4.0).roundToInt()
        return NutritionTargets(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCal,
            proteinGrams = proteinG,
            carbsGrams = carbsG,
            fatsGrams = fatsG
        )
    }

    fun validatePhoneNumber(countryCode: CountryCode, rawDigits: String): Pair<Boolean, String> {
        val digits = rawDigits.filter { it.isDigit() }
        return if (digits.length in countryCode.minDigits..countryCode.maxDigits) {
            true to "${countryCode.dialCode}$digits"
        } else {
            false to "Enter ${countryCode.minDigits}–${countryCode.maxDigits} digits for ${countryCode.name}"
        }
    }

    fun todayDateKey(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

    fun currentDayCode(millis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            else -> "SUN"
        }
    }

    fun formatInr(amount: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
        return "₹${nf.format(amount.roundToInt())}"
    }

    fun formatDateShort(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

    fun formatDateTimeShort(millis: Long): String =
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))
}
