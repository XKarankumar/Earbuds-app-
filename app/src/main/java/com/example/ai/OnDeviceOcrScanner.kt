package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OnDeviceOcrResult(
    val rawText: String,
    val extractedPhoneNumbers: List<String>,
    val scaledBitmap: Bitmap
)

object OnDeviceOcrScanner {

    private const val MAX_DIMENSION_PX = 1280

    suspend fun scanUri(
        context: Context,
        uri: Uri,
        defaultRegion: String = "IN"
    ): OnDeviceOcrResult = withContext(Dispatchers.IO) {
        val original = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalArgumentException("Could not open selected image.")

        val scaled = scaleBitmapSafely(original, MAX_DIMENSION_PX)
        if (scaled !== original) {
            original.recycle()
        }

        val recognizedText = runMlKitTextRecognition(scaled)
        val numbers = PhoneValidatorEngine.extractPhoneNumbersFromText(recognizedText, defaultRegion)

        OnDeviceOcrResult(
            rawText = recognizedText,
            extractedPhoneNumbers = numbers,
            scaledBitmap = scaled
        )
    }

    private suspend fun runMlKitTextRecognition(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    recognizer.close()
                    if (cont.isActive) cont.resume(visionText.text.trim())
                }
                .addOnFailureListener { e ->
                    recognizer.close()
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }

    private fun scaleBitmapSafely(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source

        val ratio = width.toFloat() / height.toFloat()
        val targetW: Int
        val targetH: Int
        if (width >= height) {
            targetW = maxDim
            targetH = (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            targetH = maxDim
            targetW = (maxDim * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
