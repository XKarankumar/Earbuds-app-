package com.example.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.domain.CalculatorEngine
import com.example.model.CustomerSummary
import com.example.model.DashboardMetrics
import com.example.model.RecentTransactionItem
import com.example.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LedgerPdfExporter {

    fun exportFullLedgerReport(
        context: Context,
        metrics: DashboardMetrics,
        customers: List<CustomerSummary>,
        recentTransactions: List<RecentTransactionItem>,
        currencySymbol: String,
        businessSignature: String
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val headerBg = Paint().apply { color = Color.parseColor("#071B33") }
        val accentPaint = Paint().apply { color = Color.parseColor("#00A878") }
        val titlePaint = Paint().apply {
            color = Color.parseColor("#F5FFFC")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#69E6B4")
            textSize = 11f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0878D1")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10.5f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
            isAntiAlias = true
        }

        canvas.drawRect(0f, 0f, 595f, 92f, headerBg)
        canvas.drawRect(0f, 88f, 595f, 92f, accentPaint)
        canvas.drawText("Simple SMS Calculator — Hisab-Kitab Report", 32f, 40f, titlePaint)
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Ocean Emerald 3.5 • Prepared by $businessSignature • $dateStr", 32f, 64f, subtitlePaint)

        var y = 124f
        canvas.drawText("Ledger Summary", 32f, y, sectionPaint)
        y += 22f
        canvas.drawText(
            "Total Jama (Received): ${CalculatorEngine.formatCurrency(metrics.totalJama, currencySymbol)}",
            32f,
            y,
            bodyPaint
        )
        y += 18f
        canvas.drawText(
            "Total Udhaar (Given): ${CalculatorEngine.formatCurrency(metrics.totalUdhaar, currencySymbol)}",
            32f,
            y,
            bodyPaint
        )
        y += 18f
        canvas.drawText(
            "Remaining Balance: ${CalculatorEngine.formatCurrency(metrics.netBalance, currencySymbol)} (${metrics.customerCount} customers)",
            32f,
            y,
            bodyPaint
        )

        y += 30f
        canvas.drawText("Customer Balances", 32f, y, sectionPaint)
        y += 20f
        if (customers.isEmpty()) {
            canvas.drawText("No customers recorded yet.", 32f, y, mutedPaint)
            y += 18f
        } else {
            customers.take(15).forEach { customer ->
                if (y > 760f) return@forEach
                val balText = CalculatorEngine.formatCurrency(customer.netBalance, currencySymbol)
                canvas.drawText(
                    "${customer.name} (${customer.phone.ifBlank { "No phone" }}) — Net: $balText",
                    32f,
                    y,
                    bodyPaint
                )
                y += 16f
            }
        }

        if (y < 700f && recentTransactions.isNotEmpty()) {
            y += 16f
            canvas.drawText("Recent Transactions", 32f, y, sectionPaint)
            y += 20f
            val txFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            recentTransactions.take(14).forEach { tx ->
                if (y > 780f) return@forEach
                val sign = if (tx.type == TransactionType.JAMA) "+Jama" else "-Udhaar"
                val amt = CalculatorEngine.formatCurrency(tx.amount, currencySymbol)
                val d = txFormat.format(Date(tx.createdAtMillis))
                canvas.drawText(
                    "$d • ${tx.customerName} • $sign $amt ${if (tx.note.isNotBlank()) "(${tx.note})" else ""}",
                    32f,
                    y,
                    mutedPaint
                )
                y += 15f
            }
        }

        canvas.drawText(
            "Simple SMS Calculator • © 2026 Hardik Kumar. All Rights Reserved.",
            32f,
            816f,
            mutedPaint
        )

        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(dir, "hisab_kitab_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }
}
