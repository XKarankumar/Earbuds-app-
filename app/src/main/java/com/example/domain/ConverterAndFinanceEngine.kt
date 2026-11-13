package com.example.domain

import kotlin.math.pow

enum class UnitCategory(val label: String) {
    LENGTH("Length"),
    WEIGHT("Weight"),
    AREA("Area"),
    VOLUME("Volume"),
    TEMPERATURE("Temp"),
    DATA("Data"),
    SPEED("Speed"),
    TIME("Time")
}

data class UnitSpec(
    val id: String,
    val symbol: String,
    val label: String,
    val toBaseFactor: Double
)

data class GstResult(
    val baseAmount: Double,
    val cgst: Double,
    val sgst: Double,
    val totalGst: Double,
    val totalAmount: Double
)

data class EmiResult(
    val monthlyEmi: Double,
    val totalInterest: Double,
    val totalPayable: Double
)

data class DiscountResult(
    val originalPrice: Double,
    val savings: Double,
    val priceAfterDiscount: Double,
    val taxAmount: Double,
    val finalPrice: Double
)

data class InterestResult(
    val principal: Double,
    val interestEarned: Double,
    val maturityAmount: Double
)

object ConverterAndFinanceEngine {

    val unitCatalog: Map<UnitCategory, List<UnitSpec>> = mapOf(
        UnitCategory.LENGTH to listOf(
            UnitSpec("mm", "mm", "Millimeter", 0.001),
            UnitSpec("cm", "cm", "Centimeter", 0.01),
            UnitSpec("m", "m", "Meter", 1.0),
            UnitSpec("km", "km", "Kilometer", 1000.0),
            UnitSpec("in", "in", "Inch", 0.0254),
            UnitSpec("ft", "ft", "Foot", 0.3048),
            UnitSpec("yd", "yd", "Yard", 0.9144),
            UnitSpec("mi", "mi", "Mile", 1609.344)
        ),
        UnitCategory.WEIGHT to listOf(
            UnitSpec("mg", "mg", "Milligram", 0.000001),
            UnitSpec("g", "g", "Gram", 0.001),
            UnitSpec("kg", "kg", "Kilogram", 1.0),
            UnitSpec("q", "q", "Quintal", 100.0),
            UnitSpec("t", "t", "Metric Ton", 1000.0),
            UnitSpec("oz", "oz", "Ounce", 0.0283495),
            UnitSpec("lb", "lb", "Pound", 0.45359237)
        ),
        UnitCategory.AREA to listOf(
            UnitSpec("sqft", "sq ft", "Square Foot", 0.09290304),
            UnitSpec("sqm", "sq m", "Square Meter", 1.0),
            UnitSpec("sqyd", "sq yd", "Square Yard (Gaj)", 0.83612736),
            UnitSpec("acre", "acre", "Acre", 4046.85642),
            UnitSpec("ha", "ha", "Hectare", 10000.0),
            UnitSpec("bigha", "bigha", "Bigha (Std)", 2529.28526)
        ),
        UnitCategory.VOLUME to listOf(
            UnitSpec("ml", "ml", "Milliliter", 0.001),
            UnitSpec("l", "L", "Liter", 1.0),
            UnitSpec("gal", "gal", "Gallon (US)", 3.78541178),
            UnitSpec("cup", "cup", "Cup", 0.23658824),
            UnitSpec("floz", "fl oz", "Fluid Ounce", 0.02957353)
        ),
        UnitCategory.TEMPERATURE to listOf(
            UnitSpec("c", "°C", "Celsius", 1.0),
            UnitSpec("f", "°F", "Fahrenheit", 1.0),
            UnitSpec("k", "K", "Kelvin", 1.0)
        ),
        UnitCategory.DATA to listOf(
            UnitSpec("b", "B", "Byte", 1.0),
            UnitSpec("kb", "KB", "Kilobyte", 1024.0),
            UnitSpec("mb", "MB", "Megabyte", 1048576.0),
            UnitSpec("gb", "GB", "Gigabyte", 1073741824.0),
            UnitSpec("tb", "TB", "Terabyte", 1099511627776.0)
        ),
        UnitCategory.SPEED to listOf(
            UnitSpec("kmh", "km/h", "Km / Hour", 0.27777778),
            UnitSpec("ms", "m/s", "Meter / Sec", 1.0),
            UnitSpec("mph", "mph", "Miles / Hour", 0.44704),
            UnitSpec("kn", "kn", "Knot", 0.51444444)
        ),
        UnitCategory.TIME to listOf(
            UnitSpec("s", "sec", "Second", 1.0),
            UnitSpec("m", "min", "Minute", 60.0),
            UnitSpec("h", "hr", "Hour", 3600.0),
            UnitSpec("d", "day", "Day", 86400.0),
            UnitSpec("w", "wk", "Week", 604800.0)
        )
    )

    fun convert(
        category: UnitCategory,
        value: Double,
        from: UnitSpec,
        to: UnitSpec
    ): Double {
        if (category == UnitCategory.TEMPERATURE) {
            val celsius = when (from.id) {
                "c" -> value
                "f" -> (value - 32.0) * 5.0 / 9.0
                "k" -> value - 273.15
                else -> value
            }
            return when (to.id) {
                "c" -> celsius
                "f" -> celsius * 9.0 / 5.0 + 32.0
                "k" -> celsius + 273.15
                else -> celsius
            }
        }
        val baseValue = value * from.toBaseFactor
        return baseValue / to.toBaseFactor
    }

    fun calculateGst(amount: Double, ratePercent: Double, isInclusive: Boolean): GstResult {
        val safeAmount = amount.coerceAtLeast(0.0)
        val safeRate = ratePercent.coerceAtLeast(0.0)
        return if (isInclusive) {
            val base = safeAmount / (1.0 + safeRate / 100.0)
            val totalGst = safeAmount - base
            GstResult(
                baseAmount = base,
                cgst = totalGst / 2.0,
                sgst = totalGst / 2.0,
                totalGst = totalGst,
                totalAmount = safeAmount
            )
        } else {
            val totalGst = safeAmount * (safeRate / 100.0)
            GstResult(
                baseAmount = safeAmount,
                cgst = totalGst / 2.0,
                sgst = totalGst / 2.0,
                totalGst = totalGst,
                totalAmount = safeAmount + totalGst
            )
        }
    }

    fun calculateEmi(principal: Double, annualRatePercent: Double, tenureMonths: Int): EmiResult {
        val p = principal.coerceAtLeast(0.0)
        val n = tenureMonths.coerceAtLeast(1)
        val monthlyRate = (annualRatePercent.coerceAtLeast(0.0) / 12.0) / 100.0
        val emi = if (monthlyRate == 0.0) {
            p / n
        } else {
            val factor = (1.0 + monthlyRate).pow(n)
            (p * monthlyRate * factor) / (factor - 1.0)
        }
        val totalPayable = emi * n
        return EmiResult(
            monthlyEmi = emi,
            totalInterest = (totalPayable - p).coerceAtLeast(0.0),
            totalPayable = totalPayable
        )
    }

    fun calculateDiscount(originalPrice: Double, discountPercent: Double, extraTaxPercent: Double): DiscountResult {
        val p = originalPrice.coerceAtLeast(0.0)
        val disc = discountPercent.coerceIn(0.0, 100.0)
        val tax = extraTaxPercent.coerceAtLeast(0.0)
        val savings = p * (disc / 100.0)
        val afterDisc = p - savings
        val taxAmt = afterDisc * (tax / 100.0)
        return DiscountResult(
            originalPrice = p,
            savings = savings,
            priceAfterDiscount = afterDisc,
            taxAmount = taxAmt,
            finalPrice = afterDisc + taxAmt
        )
    }

    fun calculateInterest(
        principal: Double,
        annualRatePercent: Double,
        years: Double,
        isCompound: Boolean
    ): InterestResult {
        val p = principal.coerceAtLeast(0.0)
        val r = annualRatePercent.coerceAtLeast(0.0) / 100.0
        val t = years.coerceAtLeast(0.0)
        val maturity = if (isCompound) {
            p * (1.0 + r).pow(t)
        } else {
            p + (p * r * t)
        }
        return InterestResult(
            principal = p,
            interestEarned = (maturity - p).coerceAtLeast(0.0),
            maturityAmount = maturity
        )
    }
}
