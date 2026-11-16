package com.example.desicalc

import com.example.desicalc.engine.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun `basic arithmetic handles precedence brackets and percentages`() {
        val r1 = CalculatorEngine.evaluateExpression("1200 + 300 * 2 - (100 / 2)").getOrThrow()
        assertEquals(1750.0, r1, 0.001)

        val r2 = CalculatorEngine.evaluateExpression("2000 * 18%").getOrThrow()
        assertEquals(360.0, r2, 0.001)
    }

    @Test
    fun `division by zero returns failure cleanly`() {
        val result = CalculatorEngine.evaluateExpression("500 / 0")
        assertTrue(result.isFailure)
    }

    @Test
    fun `gst exclusive and inclusive calculations are accurate`() {
        val excl = CalculatorEngine.calculateGst(1000.0, 18.0, isInclusive = false)
        assertEquals(1000.0, excl.baseAmount, 0.01)
        assertEquals(180.0, excl.gstAmount, 0.01)
        assertEquals(90.0, excl.cgstAmount, 0.01)
        assertEquals(90.0, excl.sgstAmount, 0.01)
        assertEquals(1180.0, excl.totalAmount, 0.01)

        val incl = CalculatorEngine.calculateGst(1180.0, 18.0, isInclusive = true)
        assertEquals(1000.0, incl.baseAmount, 0.01)
        assertEquals(180.0, incl.gstAmount, 0.01)
    }

    @Test
    fun `discount and profit loss calculations are exact`() {
        val disc = CalculatorEngine.calculateDiscount(1000.0, 20.0, 10.0)
        assertEquals(720.0, disc.finalPrice, 0.01)
        assertEquals(280.0, disc.totalDiscountAmount, 0.01)

        val pl = CalculatorEngine.calculateProfitLoss(400.0, 500.0, 5.0)
        assertTrue(pl.isProfit)
        assertEquals(500.0, pl.profitOrLoss, 0.01)
        assertEquals(25.0, pl.profitPercentOnCost, 0.01)
    }

    @Test
    fun `indian unit conversions work accurately`() {
        val kg = CalculatorEngine.allUnits.first { it.id == "kg" }
        val quintal = CalculatorEngine.allUnits.first { it.id == "quintal" }
        assertEquals(2.5, CalculatorEngine.convertUnit(250.0, kg, quintal), 0.0001)

        val sqyd = CalculatorEngine.allUnits.first { it.id == "sqyd" }
        val sqft = CalculatorEngine.allUnits.first { it.id == "sqft" }
        assertEquals(900.0, CalculatorEngine.convertUnit(100.0, sqyd, sqft), 0.0001)
    }
}
