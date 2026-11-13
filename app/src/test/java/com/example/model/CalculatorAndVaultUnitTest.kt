package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorAndVaultUnitTest {

    @Test
    fun calculatorEvaluator_handlesPrecedenceAndFunctions() {
        assertEquals("14", CalculatorEvaluator.evaluate("2+3*4").getOrThrow())
        assertEquals("20", CalculatorEvaluator.evaluate("(2+3)×4").getOrThrow())
        assertEquals("9", CalculatorEvaluator.evaluate("√81").getOrThrow())
        assertTrue(CalculatorEvaluator.evaluate("5/0").isFailure)
    }

    @Test
    fun unitConverter_handlesKlLitreKgGm() {
        val litres = UnitConverterEngine.convert(2.5, UnitCategory.VOLUME, "kl", "l")
        assertEquals(2500.0, litres, 0.001)

        val grams = UnitConverterEngine.convert(3.25, UnitCategory.WEIGHT, "kg", "gm")
        assertEquals(3250.0, grams, 0.001)
    }

    @Test
    fun financialCalculators_gstDiscountInterestEmi() {
        val gst = FinancialCalculators.calculateGst(1000.0, 18.0, inclusive = false)
        assertEquals(1180.0, gst.totalAmount, 0.01)
        assertEquals(90.0, gst.cgstAmount, 0.01)

        val disc = FinancialCalculators.calculateDiscount(2000.0, 10.0, 0.0)
        assertEquals(1800.0, disc.finalPrice, 0.01)

        val si = FinancialCalculators.calculateInterest(10000.0, 10.0, 2.0, compound = false)
        assertEquals(12000.0, si.totalAmount, 0.01)

        val emi = FinancialCalculators.calculateEmi(100000.0, 12.0, 12)
        assertEquals(8884.88, emi.monthlyEmi, 0.5)
    }
}
