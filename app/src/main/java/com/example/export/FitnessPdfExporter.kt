package com.example.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.PaymentEntity
import com.example.domain.FitnessCalculators
import com.example.domain.QrCodeGenerator
import com.example.model.MemberView
import java.io.File
import java.io.FileOutputStream

object FitnessPdfExporter {

    fun exportPaymentReceipt(
        context: Context,
        payment: PaymentEntity,
        member: MemberView?,
        gymName: String,
        ownerSignature: String
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val darkHeader = Paint().apply { color = Color.parseColor("#0A0A0D") }
        val cyanBar = Paint().apply { color = Color.parseColor("#00E5FF") }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#67E8F9")
            textSize = 11f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 11.5f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            isAntiAlias = true
        }

        canvas.drawRect(0f, 0f, 595f, 100f, darkHeader)
        canvas.drawRect(0f, 96f, 595f, 100f, cyanBar)
        canvas.drawText("$gymName — OFFICIAL FEE RECEIPT", 32f, 44f, titlePaint)
        canvas.drawText(
            "Receipt #${payment.receiptNumber}  •  Issued ${FitnessCalculators.formatDateTimeShort(payment.paidAtMillis)}",
            32f,
            70f,
            subtitlePaint
        )

        var y = 140f
        canvas.drawText("Member Details", 32f, y, sectionPaint)
        y += 24f
        canvas.drawText("Member Name: ${payment.memberName}", 32f, y, bodyPaint); y += 20f
        canvas.drawText("Member ID: ${member?.memberCode ?: "FH-MEM-${payment.memberId}"}", 32f, y, bodyPaint); y += 20f
        if (member != null) {
            canvas.drawText("Mobile: ${member.countryCode} ••••••${member.phoneLast4}", 32f, y, bodyPaint); y += 20f
            canvas.drawText("Membership Expiry: ${FitnessCalculators.formatDateShort(member.expiryDateMillis)}", 32f, y, bodyPaint); y += 20f
        }

        y += 18f
        canvas.drawText("Payment Breakdown", 32f, y, sectionPaint); y += 24f
        canvas.drawText("Membership Plan: ${payment.planType}", 32f, y, bodyPaint); y += 20f
        canvas.drawText("Billing Period: ${payment.periodLabel}", 32f, y, bodyPaint); y += 20f
        canvas.drawText("Payment Method: ${payment.paymentMethod}", 32f, y, bodyPaint); y += 20f
        canvas.drawText("Status: ${if (payment.isPaid) "PAID IN FULL" else "PENDING"}", 32f, y, bodyPaint); y += 28f

        val amountPaint = Paint().apply {
            color = Color.parseColor("#059669")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Total Paid: ${FitnessCalculators.formatInr(payment.amount)}", 32f, y, amountPaint)

        val qrBmp = QrCodeGenerator.generateQrBitmap("FITNESSHUB:RCP:${payment.receiptNumber}:${payment.amount}", 140)
        if (qrBmp != null) {
            canvas.drawBitmap(qrBmp, 420f, 136f, null)
            canvas.drawText("Scan to verify receipt", 432f, 290f, mutedPaint)
        }

        canvas.drawText("Authorized By: $ownerSignature  •  $gymName v1.0", 32f, 790f, mutedPaint)
        canvas.drawText("© 2026 Hardik Kumar. All Rights Reserved.", 32f, 808f, mutedPaint)

        doc.finishPage(page)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "receipt_${payment.receiptNumber}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun exportMonthlyRevenueReport(
        context: Context,
        payments: List<PaymentEntity>,
        activeMembersCount: Int,
        pendingAmount: Double,
        gymName: String,
        ownerSignature: String
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val darkHeader = Paint().apply { color = Color.parseColor("#0A0A0D") }
        val accent = Paint().apply { color = Color.parseColor("#10B981") }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#6EE7B7")
            textSize = 11f
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 10.5f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            isAntiAlias = true
        }

        canvas.drawRect(0f, 0f, 595f, 92f, darkHeader)
        canvas.drawRect(0f, 88f, 595f, 92f, accent)
        canvas.drawText("$gymName — MONTHLY REVENUE REPORT", 32f, 40f, titlePaint)
        canvas.drawText(
            "Prepared by $ownerSignature • ${FitnessCalculators.formatDateTimeShort(System.currentTimeMillis())}",
            32f,
            64f,
            subtitlePaint
        )

        val collected = payments.filter { it.isPaid }.sumOf { it.amount }
        var y = 128f
        canvas.drawText("Executive Summary", 32f, y, sectionPaint); y += 22f
        canvas.drawText("Active Members: $activeMembersCount", 32f, y, bodyPaint); y += 18f
        canvas.drawText("Total Collected Revenue: ${FitnessCalculators.formatInr(collected)}", 32f, y, bodyPaint); y += 18f
        canvas.drawText("Pending Dues: ${FitnessCalculators.formatInr(pendingAmount)}", 32f, y, bodyPaint); y += 28f

        canvas.drawText("Recent Fee Transactions", 32f, y, sectionPaint); y += 20f
        payments.take(24).forEach { p ->
            if (y > 770f) return@forEach
            val status = if (p.isPaid) "PAID" else "PENDING"
            canvas.drawText(
                "${p.receiptNumber} • ${p.memberName} • ${p.planType} • ${p.paymentMethod} • ${FitnessCalculators.formatInr(p.amount)} ($status)",
                32f,
                y,
                bodyPaint
            )
            y += 16f
        }

        canvas.drawText("$gymName v1.0 • Developer: Hardik Kumar", 32f, 812f, mutedPaint)
        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "revenue_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
