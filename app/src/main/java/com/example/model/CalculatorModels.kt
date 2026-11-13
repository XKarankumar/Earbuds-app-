package com.example.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow
import kotlin.math.sqrt

object CalculatorEvaluator {

    fun evaluate(rawExpression: String): Result<String> {
        val cleaned = rawExpression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(",", "")
            .trim()
        if (cleaned.isEmpty()) return Result.success("0")

        return try {
            val parser = ExpressionParser(cleaned)
            val value = parser.parse()
            if (value.isNaN() || value.isInfinite()) {
                Result.failure(ArithmeticException("Cannot divide by zero"))
            } else {
                Result.success(formatNumber(value))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val bd = BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
        val plain = bd.toPlainString()
        return if (plain == "-0") "0" else plain
    }

    private class ExpressionParser(private val input: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) throw IllegalArgumentException("Unexpected: ${ch.toChar()}")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> {
                        val right = parseTerm()
                        x += right
                    }
                    eat('-'.code) -> {
                        val right = parseTerm()
                        x -= right
                    }
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val denom = parseFactor()
                        if (denom == 0.0) throw ArithmeticException("Division by zero")
                        x /= denom
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            when {
                eat('('.code) -> {
                    x = parseExpression()
                    if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
                }
                eat('√'.code) -> {
                    val inner = parseFactor()
                    if (inner < 0) throw ArithmeticException("Negative square root")
                    x = sqrt(inner)
                }
                ch in '0'.code..'9'.code || ch == '.'.code -> {
                    while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                    val numStr = input.substring(startPos, pos)
                    x = numStr.toDouble()
                }
                else -> throw IllegalArgumentException("Unexpected char: ${if (ch >= 0) ch.toChar() else ' '}")
            }

            if (eat('%'.code)) {
                x /= 100.0
            }
            if (eat('^'.code)) {
                x = x.pow(parseFactor())
            }
            return x
        }
    }
}

enum class UnitCategory(val title: String) {
    VOLUME("Volume (KL / L)"),
    WEIGHT("Weight (KG / GM)"),
    LENGTH("Length"),
    AREA("Land & Area"),
    TEMPERATURE("Temperature")
}

data class ConversionUnit(
    val id: String,
    val label: String,
    val symbol: String,
    val toBaseFactor: Double // base: L for Volume, g for Weight, m for Length, sq.m for Area
)

object UnitConverterEngine {
    val unitsByCategory: Map<UnitCategory, List<ConversionUnit>> = mapOf(
        UnitCategory.VOLUME to listOf(
            ConversionUnit("kl", "Kilolitre", "KL", 1000.0),
            ConversionUnit("l", "Litre", "L", 1.0),
            ConversionUnit("ml", "Millilitre", "mL", 0.001),
            ConversionUnit("gal", "Gallon (US)", "gal", 3.785411784),
            ConversionUnit("m3", "Cubic Meter", "m³", 1000.0)
        ),
        UnitCategory.WEIGHT to listOf(
            ConversionUnit("mt", "Metric Ton", "MT", 1_000_000.0),
            ConversionUnit("qtl", "Quintal", "Qtl", 100_000.0),
            ConversionUnit("kg", "Kilogram", "KG", 1000.0),
            ConversionUnit("gm", "Gram", "GM", 1.0),
            ConversionUnit("mg", "Milligram", "mg", 0.001),
            ConversionUnit("lb", "Pound", "lb", 453.59237),
            ConversionUnit("oz", "Ounce", "oz", 28.349523125)
        ),
        UnitCategory.LENGTH to listOf(
            ConversionUnit("km", "Kilometre", "KM", 1000.0),
            ConversionUnit("m", "Metre", "m", 1.0),
            ConversionUnit("cm", "Centimetre", "cm", 0.01),
            ConversionUnit("mm", "Millimetre", "mm", 0.001),
            ConversionUnit("ft", "Foot", "ft", 0.3048),
            ConversionUnit("in", "Inch", "in", 0.0254),
            ConversionUnit("yd", "Yard", "yd", 0.9144),
            ConversionUnit("mi", "Mile", "mi", 1609.344)
        ),
        UnitCategory.AREA to listOf(
            ConversionUnit("sqft", "Square Foot", "sq.ft", 0.09290304),
            ConversionUnit("sqm", "Square Metre", "sq.m", 1.0),
            ConversionUnit("gaj", "Sq. Yard / Gaj", "gaj", 0.83612736),
            ConversionUnit("acre", "Acre", "acre", 4046.8564224),
            ConversionUnit("hectare", "Hectare", "ha", 10000.0),
            ConversionUnit("bigha", "Bigha (Standard)", "bigha", 2529.285264)
        ),
        UnitCategory.TEMPERATURE to listOf(
            ConversionUnit("c", "Celsius", "°C", 1.0),
            ConversionUnit("f", "Fahrenheit", "°F", 1.0),
            ConversionUnit("k", "Kelvin", "K", 1.0)
        )
    )

    fun convert(
        value: Double,
        category: UnitCategory,
        fromId: String,
        toId: String
    ): Double {
        if (category == UnitCategory.TEMPERATURE) {
            val celsius = when (fromId) {
                "f" -> (value - 32.0) * 5.0 / 9.0
                "k" -> value - 273.15
                else -> value
            }
            return when (toId) {
                "f" -> celsius * 9.0 / 5.0 + 32.0
                "k" -> celsius + 273.15
                else -> celsius
            }
        }
        val list = unitsByCategory[category] ?: return value
        val from = list.firstOrNull { it.id == fromId } ?: return value
        val to = list.firstOrNull { it.id == toId } ?: return value
        val baseValue = value * from.toBaseFactor
        return baseValue / to.toBaseFactor
    }
}

data class GstResult(
    val netAmount: Double,
    val gstAmount: Double,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val totalAmount: Double
)

data class InterestResult(
    val principal: Double,
    val interest: Double,
    val totalAmount: Double
)

data class DiscountResult(
    val originalPrice: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val finalPrice: Double
)

data class EmiResult(
    val monthlyEmi: Double,
    val totalInterest: Double,
    val totalPayable: Double,
    val principalPercent: Float
)

object FinancialCalculators {

    fun calculateGst(amount: Double, ratePercent: Double, inclusive: Boolean): GstResult {
        if (amount <= 0.0) return GstResult(0.0, 0.0, 0.0, 0.0, 0.0)
        val rate = ratePercent / 100.0
        val net: Double
        val gst: Double
        val total: Double
        if (inclusive) {
            total = amount
            net = amount / (1.0 + rate)
            gst = total - net
        } else {
            net = amount
            gst = amount * rate
            total = net + gst
        }
        return GstResult(
            netAmount = net,
            gstAmount = gst,
            cgstAmount = gst / 2.0,
            sgstAmount = gst / 2.0,
            totalAmount = total
        )
    }

    fun calculateInterest(
        principal: Double,
        annualRatePercent: Double,
        years: Double,
        compound: Boolean,
        compoundsPerYear: Int = 1
    ): InterestResult {
        if (principal <= 0.0 || years <= 0.0) return InterestResult(principal, 0.0, principal)
        val r = annualRatePercent / 100.0
        val total = if (!compound) {
            principal * (1.0 + r * years)
        } else {
            val n = compoundsPerYear.coerceAtLeast(1).toDouble()
            principal * (1.0 + r / n).pow(n * years)
        }
        return InterestResult(
            principal = principal,
            interest = (total - principal).coerceAtLeast(0.0),
            totalAmount = total
        )
    }

    fun calculateDiscount(
        originalPrice: Double,
        discountPercent: Double,
        extraTaxPercent: Double
    ): DiscountResult {
        if (originalPrice <= 0.0) return DiscountResult(0.0, 0.0, 0.0, 0.0)
        val discount = originalPrice * (discountPercent.coerceIn(0.0, 100.0) / 100.0)
        val discounted = (originalPrice - discount).coerceAtLeast(0.0)
        val tax = discounted * (extraTaxPercent.coerceAtLeast(0.0) / 100.0)
        return DiscountResult(
            originalPrice = originalPrice,
            discountAmount = discount,
            taxAmount = tax,
            finalPrice = discounted + tax
        )
    }

    fun calculateEmi(
        principal: Double,
        annualRatePercent: Double,
        months: Int
    ): EmiResult {
        if (principal <= 0.0 || months <= 0) return EmiResult(0.0, 0.0, 0.0, 1f)
        val monthlyRate = annualRatePercent / 12.0 / 100.0
        val emi = if (monthlyRate == 0.0) {
            principal / months
        } else {
            val factor = (1.0 + monthlyRate).pow(months.toDouble())
            principal * monthlyRate * factor / (factor - 1.0)
        }
        val totalPayable = emi * months
        val totalInterest = (totalPayable - principal).coerceAtLeast(0.0)
        val principalPct = if (totalPayable > 0.0) (principal / totalPayable).toFloat().coerceIn(0f, 1f) else 1f
        return EmiResult(
            monthlyEmi = emi,
            totalInterest = totalInterest,
            totalPayable = totalPayable,
            principalPercent = principalPct
        )
    }
}
