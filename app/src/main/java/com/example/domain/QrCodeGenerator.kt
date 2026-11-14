package com.example.domain

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {

    fun buildMemberQrPayload(memberCode: String): String = "FITNESSHUB:MEM:${memberCode.trim()}"

    fun parseMemberCodeFromQr(rawPayload: String): String {
        val trimmed = rawPayload.trim()
        return when {
            trimmed.startsWith("FITNESSHUB:MEM:", ignoreCase = true) ->
                trimmed.substringAfter("FITNESSHUB:MEM:").trim()
            else -> trimmed
        }
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (_: Throwable) {
            null
        }
    }
}
