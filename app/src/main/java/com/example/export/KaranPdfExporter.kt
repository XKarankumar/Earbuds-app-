package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.SearchHistoryRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KaranPdfExporter {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH)

    suspend fun exportRecordsToPdf(
        context: Context,
        reportTitle: String,
        records: List<SearchHistoryRecord>
    ): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val pdfFile = File(exportDir, "karan_finder_report_$stamp.pdf")

        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = pageWidth - margin * 2

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#0284C7")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 10.5f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 48f

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = 48f
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - 48f) {
                newPage()
            }
        }

        fun drawWrapped(text: String, paint: Paint, lineHeight: Float = 15f) {
            val paragraphs = text.split("\n")
            for (para in paragraphs) {
                val words = para.split(" ")
                var line = StringBuilder()
                for (word in words) {
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(candidate) > contentWidth) {
                        ensureSpace(lineHeight)
                        canvas.drawText(line.toString(), margin, y, paint)
                        y += lineHeight
                        line = StringBuilder(word)
                    } else {
                        line = StringBuilder(candidate)
                    }
                }
                if (line.isNotEmpty()) {
                    ensureSpace(lineHeight)
                    canvas.drawText(line.toString(), margin, y, paint)
                    y += lineHeight
                }
            }
        }

        // Header
        canvas.drawText("Karan Number Finder Pro AI", margin, y, titlePaint)
        y += 18f
        canvas.drawText(
            "$reportTitle • Developer: Hardik Kumar • v1.0",
            margin,
            y,
            subtitlePaint
        )
        y += 14f
        canvas.drawText(
            "Generated: ${dateFormatter.format(Date())} • Privacy-Compliant Report",
            margin,
            y,
            mutedPaint
        )
        y += 14f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 20f

        if (records.isEmpty()) {
            drawWrapped("No records included in this report.", bodyPaint)
        } else {
            records.forEachIndexed { index, record ->
                ensureSpace(95f)
                canvas.drawText(
                    "${index + 1}. [${record.recordType.label}] ${record.title}",
                    margin,
                    y,
                    headingPaint
                )
                y += 16f
                canvas.drawText(
                    "Risk: ${record.riskLevel} (${record.riskScore}/100) • ${dateFormatter.format(Date(record.createdAtMillis))}",
                    margin,
                    y,
                    subtitlePaint
                )
                y += 15f
                if (record.subtitle.isNotBlank()) {
                    drawWrapped(record.subtitle, bodyPaint)
                }
                drawWrapped(record.summaryText, bodyPaint)
                drawWrapped("Source: ${record.sourceAttribution}", mutedPaint, 13f)
                y += 8f
                canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
                y += 16f
            }
        }

        ensureSpace(36f)
        drawWrapped(
            "Disclaimer: Reports strictly omit private addresses, Aadhaar, banking credentials, and live location data in compliance with Indian privacy laws.",
            mutedPaint,
            13f
        )

        doc.finishPage(page)
        FileOutputStream(pdfFile).use { out ->
            doc.writeTo(out)
        }
        doc.close()
        pdfFile
    }

    fun sharePdfFile(context: Context, file: File, title: String = "Karan Number Finder Pro AI Report") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share PDF Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
