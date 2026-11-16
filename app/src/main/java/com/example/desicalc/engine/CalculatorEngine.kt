package com.example.desicalc.engine

import kotlin.math.abs

data class GstResult(
    val baseAmount: Double,
    val gstAmount: Double,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val totalAmount: Double
)

data class DiscountResult(
    val originalPrice: Double,
    val totalDiscountAmount: Double,
    val effectiveDiscountPercent: Double,
    val finalPrice: Double
)

data class ProfitLossResult(
    val costTotal: Double,
    val sellTotal: Double,
    val profitOrLoss: Double,
    val isProfit: Boolean,
    val profitPercentOnCost: Double,
    val marginPercentOnSell: Double
)

enum class UnitCategory(val title: String) {
    WEIGHT("Weight / वजन"),
    LENGTH("Length / लंबाई"),
    AREA("Land & Area / क्षेत्रफल"),
    VOLUME("Volume / आयतन")
}

data class UnitDef(
    val id: String,
    val label: String,
    val category: UnitCategory,
    val toBaseFactor: Double // base: gram, meter, sq.ft, milliliter
)

object CalculatorEngine {

    val allUnits: List<UnitDef> = listOf(
        // Weight (base: gram)
        UnitDef("kg", "Kilogram (kg)", UnitCategory.WEIGHT, 1000.0),
        UnitDef("g", "Gram (g)", UnitCategory.WEIGHT, 1.0),
        UnitDef("quintal", "Quintal (क्विंटल)", UnitCategory.WEIGHT, 100_000.0),
        UnitDef("ton", "Metric Ton (टन)", UnitCategory.WEIGHT, 1_000_000.0),
        UnitDef("tola", "Tola (तोला 11.664g)", UnitCategory.WEIGHT, 11.6638038),
        UnitDef("lb", "Pound (lb)", UnitCategory.WEIGHT, 453.59237),
        // Length (base: meter)
        UnitDef("m", "Meter (m)", UnitCategory.LENGTH, 1.0),
        UnitDef("cm", "Centimeter (cm)", UnitCategory.LENGTH, 0.01),
        UnitDef("km", "Kilometer (km)", UnitCategory.LENGTH, 1000.0),
        UnitDef("ft", "Foot (ft)", UnitCategory.LENGTH, 0.3048),
        UnitDef("inch", "Inch (in)", UnitCategory.LENGTH, 0.0254),
        UnitDef("yard", "Yard / Gaj (गज)", UnitCategory.LENGTH, 0.9144),
        // Area (base: sq ft)
        UnitDef("sqft", "Sq. Feet (sq.ft)", UnitCategory.AREA, 1.0),
        UnitDef("sqyd", "Sq. Yard / Gaj (वर्ग गज)", UnitCategory.AREA, 9.0),
        UnitDef("sqm", "Sq. Meter (sq.m)", UnitCategory.AREA, 10.7639104),
        UnitDef("acre", "Acre (एकड़)", UnitCategory.AREA, 43_560.0),
        UnitDef("bigha", "Bigha (बीघा ~27,225 sq.ft)", UnitCategory.AREA, 27_225.0),
        // Volume (base: ml)
        UnitDef("l", "Litre (L)", UnitCategory.VOLUME, 1000.0),
        UnitDef("ml", "Millilitre (mL)", UnitCategory.VOLUME, 1.0)
    )

    fun evaluateExpression(rawInput: String): Result<Double> {
        return runCatching {
            val sanitized = rawInput
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace(",", "")
                .replace("₹", "")
                .trim()
            require(sanitized.isNotEmpty()) { "Empty expression" }
            val parser = RecursiveParser(sanitized)
            val value = parser.parseExpression()
            require(!parser.hasRemaining()) { "Unexpected token at '${parser.remaining()}'" }
            require(value.isFinite()) { "Result is not finite (division by zero)" }
            value
        }
    }

    fun formatCleanNumber(value: Double): String {
        if (!value.isFinite()) return "Error"
        val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
        return if (abs(rounded % 1.0) < 1e-7) {
            rounded.toLong().toString()
        } else {
            rounded.toBigDecimal().stripTrailingZeros().toPlainString()
        }
    }

    fun calculateGst(amount: Double, ratePercent: Double, isInclusive: Boolean): GstResult {
        require(amount >= 0.0 && ratePercent >= 0.0) { "Amount and GST rate must be non-negative" }
        return if (isInclusive) {
            val base = amount / (1.0 + ratePercent / 100.0)
            val gst = amount - base
            GstResult(
                baseAmount = base,
                gstAmount = gst,
                cgstAmount = gst / 2.0,
                sgstAmount = gst / 2.0,
                totalAmount = amount
            )
        } else {
            val gst = amount * (ratePercent / 100.0)
            val total = amount + gst
            GstResult(
                baseAmount = amount,
                gstAmount = gst,
                cgstAmount = gst / 2.0,
                sgstAmount = gst / 2.0,
                totalAmount = total
            )
        }
    }

    fun calculateDiscount(
        mrp: Double,
        discountPercent: Double,
        extraDiscountPercent: Double = 0.0
    ): DiscountResult {
        require(mrp >= 0.0) { "MRP cannot be negative" }
        val d1 = discountPercent.coerceIn(0.0, 100.0)
        val d2 = extraDiscountPercent.coerceIn(0.0, 100.0)
        val afterFirst = mrp * (1.0 - d1 / 100.0)
        val finalPrice = afterFirst * (1.0 - d2 / 100.0)
        val totalDiscount = mrp - finalPrice
        val effectivePct = if (mrp > 0.0) (totalDiscount / mrp) * 100.0 else 0.0
        return DiscountResult(
            originalPrice = mrp,
            totalDiscountAmount = totalDiscount,
            effectiveDiscountPercent = effectivePct,
            finalPrice = finalPrice
        )
    }

    fun calculatePercentageOf(percent: Double, total: Double): Double = (percent / 100.0) * total

    fun calculateWhatPercent(part: Double, whole: Double): Double {
        require(whole != 0.0) { "Whole value cannot be zero" }
        return (part / whole) * 100.0
    }

    fun calculateProfitLoss(
        costPricePerUnit: Double,
        sellingPricePerUnit: Double,
        quantity: Double = 1.0
    ): ProfitLossResult {
        require(costPricePerUnit >= 0.0 && sellingPricePerUnit >= 0.0 && quantity > 0.0) {
            "Prices must be non-negative and quantity > 0"
        }
        val costTotal = costPricePerUnit * quantity
        val sellTotal = sellingPricePerUnit * quantity
        val diff = sellTotal - costTotal
        val pctCost = if (costTotal > 0.0) (diff / costTotal) * 100.0 else 0.0
        val marginSell = if (sellTotal > 0.0) (diff / sellTotal) * 100.0 else 0.0
        return ProfitLossResult(
            costTotal = costTotal,
            sellTotal = sellTotal,
            profitOrLoss = diff,
            isProfit = diff >= 0.0,
            profitPercentOnCost = pctCost,
            marginPercentOnSell = marginSell
        )
    }

    fun convertUnit(value: Double, from: UnitDef, to: UnitDef): Double {
        require(from.category == to.category) { "Units must belong to the same category" }
        val inBase = value * from.toBaseFactor
        return inBase / to.toBaseFactor
    }

    private class RecursiveParser(private val input: String) {
        private var pos = 0

        fun hasRemaining(): Boolean {
            skipWhitespace()
            return pos < input.length
        }

        fun remaining(): String = input.substring(pos)

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                skipWhitespace()
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                skipWhitespace()
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        require(abs(divisor) > 1e-12) { "Division by zero" }
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            skipWhitespace()
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                require(eat(')')) { "Missing closing parenthesis" }
            } else {
                while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                    pos++
                }
                require(pos > startPos) {
                    "Unexpected character '${if (pos < input.length) input[pos] else "EOF"}'"
                }
                x = input.substring(startPos, pos).toDouble()
            }

            skipWhitespace()
            if (eat('%')) {
                x /= 100.0
            }
            return x
        }

        private fun eat(charToEat: Char): Boolean {
            skipWhitespace()
            if (pos < input.length && input[pos] == charToEat) {
                pos++
                return true
            }
            return false
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }
    }
}
