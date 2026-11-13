package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.abs

object CalculatorEngine {

    sealed interface EvalResult {
        data class Success(val value: Double, val formatted: String) : EvalResult
        data class Error(val message: String) : EvalResult
    }

    fun evaluate(rawExpression: String): EvalResult {
        val cleaned = rawExpression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(",", "")
            .replace(" ", "")
            .trim()

        if (cleaned.isEmpty()) {
            return EvalResult.Success(0.0, "0")
        }

        return try {
            val parser = ExpressionParser(cleaned)
            val result = parser.parseExpression()
            if (parser.pos < cleaned.length) {
                EvalResult.Error("Syntax error")
            } else if (result.isNaN() || result.isInfinite()) {
                EvalResult.Error("Cannot divide by 0")
            } else {
                EvalResult.Success(result, formatNumber(result))
            }
        } catch (_: ArithmeticException) {
            EvalResult.Error("Cannot divide by 0")
        } catch (_: Exception) {
            EvalResult.Error("Invalid expression")
        }
    }

    fun tryPreview(rawExpression: String): String? {
        val trimmed = rawExpression.trim()
        if (trimmed.isEmpty()) return null
        val lastChar = trimmed.last()
        if (lastChar in listOf('+', '-', '×', '÷', '*', '/', '(')) return null
        return when (val res = evaluate(trimmed)) {
            is EvalResult.Success -> res.formatted
            is EvalResult.Error -> null
        }
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val normalized = if (abs(value) < 1e-12) 0.0 else value
        val bd = BigDecimal.valueOf(normalized)
            .setScale(8, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        val plain = bd.toPlainString()
        return if (plain == "-0") "0" else plain
    }

    fun formatCurrency(amount: Double, symbol: String): String {
        val absVal = abs(amount)
        val df = DecimalFormat("#,##0.##")
        val formatted = df.format(absVal)
        return if (amount < -0.005) "-$symbol$formatted" else "$symbol$formatted"
    }

    private class ExpressionParser(private val input: String) {
        var pos = 0

        fun parseExpression(): Double {
            var x = parseTerm()
            while (pos < input.length) {
                when (input[pos]) {
                    '+' -> {
                        pos++
                        x += parseTerm()
                    }
                    '-' -> {
                        pos++
                        x -= parseTerm()
                    }
                    else -> return x
                }
            }
            return x
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (pos < input.length) {
                when (input[pos]) {
                    '*' -> {
                        pos++
                        x *= parseFactor()
                    }
                    '/' -> {
                        pos++
                        val divisor = parseFactor()
                        if (abs(divisor) < 1e-15) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    else -> return x
                }
            }
            return x
        }

        private fun parseFactor(): Double {
            if (pos < input.length && input[pos] == '+') {
                pos++
                return parseFactor()
            }
            if (pos < input.length && input[pos] == '-') {
                pos++
                return -parseFactor()
            }

            var x: Double
            val startPos = pos
            if (pos < input.length && input[pos] == '(') {
                pos++
                x = parseExpression()
                if (pos < input.length && input[pos] == ')') {
                    pos++
                }
            } else {
                while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                    pos++
                }
                if (startPos == pos) {
                    throw IllegalArgumentException("Unexpected char")
                }
                x = input.substring(startPos, pos).toDouble()
            }

            while (pos < input.length && input[pos] == '%') {
                pos++
                x /= 100.0
            }
            return x
        }
    }
}
