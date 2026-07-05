package com.einkaufsscanner.domain.usecase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log

object ImageProcessing {
    private const val TAG = "ImageProcessing"

    // ROI (Region of Interest) - MASSIVELY expanded to cover entire price label
    const val ROI_X_START = 0.01f   // ~1% margin from left (minimal)
    const val ROI_X_END = 0.99f     // ~1% margin from right (minimal) = 98% width
    const val ROI_Y_START = 0.08f   // 8% from top (was 0.25f) - HUGE expanded
    const val ROI_Y_END = 0.92f     // 92% from top (was 0.75f) - HUGE expanded = 84% height

    fun preprocessForOcr(bitmap: Bitmap): Bitmap {
        Log.d(TAG, "Preprocessing bitmap: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")
        return try {
            var result = bitmap

            // Ensure bitmap is in ARGB_8888 format
            if (bitmap.config != Bitmap.Config.ARGB_8888) {
                result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }

            // Upscale if small (ML Kit performs better with larger images)
            if (result.height < 600) {
                val scale = 2.0f
                val newWidth = (result.width * scale).toInt()
                val newHeight = (result.height * scale).toInt()
                result = Bitmap.createScaledBitmap(result, newWidth, newHeight, true)
                Log.d(TAG, "Upscaled to ${result.width}x${result.height}")
            }

            // Convert to grayscale for better OCR
            result = toGrayscale(result)

            // Improve contrast
            result = enhanceContrast(result)

            Log.d(TAG, "Preprocessing finished: ${result.width}x${result.height}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Preprocessing failed, returning original", e)
            bitmap
        }
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.setScale(1.2f, 1.2f, 1.2f, 1f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    /**
     * Extract ROI (Region of Interest) from bitmap with custom dimensions.
     * @param widthPercent Percentage of width to use (0.0-1.0)
     * @param heightPercent Percentage of height to use (0.0-1.0)
     */
    fun extractRoi(
        bitmap: Bitmap,
        widthPercent: Float = ROI_X_END - ROI_X_START,
        heightPercent: Float = ROI_Y_END - ROI_Y_START,
    ): Bitmap {
        // Ensure bitmap is not hardware-backed before we do anything
        var currentBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && 
                           bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        Log.d(TAG, "extractRoi input: ${currentBitmap.width}x${currentBitmap.height}")

        // Step 1: If the image is landscape (from sensor), rotate it to portrait first.
        if (currentBitmap.width > currentBitmap.height) {
            Log.d(TAG, "Sensor is landscape. Rotating to portrait for ROI extraction.")
            val matrix = android.graphics.Matrix()
            matrix.postRotate(90f)
            currentBitmap = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
        }

        // Step 2: Calculate ROI with dynamic dimensions
        // Center the ROI on the image
        val xMargin = (1.0f - widthPercent) / 2.0f
        val yMargin = (1.0f - heightPercent) / 2.0f

        val x1 = (currentBitmap.width * xMargin).toInt()
        val x2 = (currentBitmap.width * (1.0f - xMargin)).toInt()
        val y1 = (currentBitmap.height * yMargin).toInt()
        val y2 = (currentBitmap.height * (1.0f - yMargin)).toInt()

        val width = (x2 - x1).coerceIn(1, currentBitmap.width - x1)
        val height = (y2 - y1).coerceIn(1, currentBitmap.height - y1)

        Log.d(TAG, "Final ROI: x=$x1, y=$y1, w=$width, h=$height (on ${currentBitmap.width}x${currentBitmap.height} portrait image)")
        return Bitmap.createBitmap(currentBitmap, x1, y1, width, height)
    }
}
