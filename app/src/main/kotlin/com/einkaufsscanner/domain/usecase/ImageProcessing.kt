package com.einkaufsscanner.domain.usecase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log

object ImageProcessing {
    private const val TAG = "ImageProcessing"
    // Higher resolution for better OCR accuracy (1200-1600px)
    // Google recommends 16-24 pixels per character height for accurate recognition
    private const val MAX_BITMAP_WIDTH = 1400  // Increased from 900 for better accuracy
    private const val MAX_BITMAP_HEIGHT = 1800

    // DEBUG: Set to true to skip ROI crop and use full image
    private const val DEBUG_SKIP_ROI_CROP = true

    // ROI (Region of Interest) - MASSIVELY expanded to cover entire price label
    const val ROI_X_START = 0.01f   // ~1% margin from left (minimal)
    const val ROI_X_END = 0.99f     // ~1% margin from right (minimal) = 98% width
    const val ROI_Y_START = 0.08f   // 8% from top (was 0.25f) - HUGE expanded
    const val ROI_Y_END = 0.92f     // 92% from top (was 0.75f) - HUGE expanded = 84% height

    /**
     * Downscale bitmap for OCR processing with HIGH QUALITY
     * Uses bilinear filtering (true) for sharp edges on text
     * Higher resolution (1200-1600px) for better OCR accuracy
     * Google recommends 16-24 pixels per character height
     */
    fun downscaleBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_BITMAP_WIDTH && bitmap.height <= MAX_BITMAP_HEIGHT) {
            return bitmap
        }

        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        val newWidth: Int
        val newHeight: Int

        if (aspectRatio > 1) {
            newWidth = MAX_BITMAP_WIDTH
            newHeight = (MAX_BITMAP_WIDTH / aspectRatio).toInt()
        } else {
            newHeight = MAX_BITMAP_HEIGHT
            newWidth = (MAX_BITMAP_HEIGHT * aspectRatio).toInt()
        }

        Log.d(TAG, "Downscaling ${bitmap.width}x${bitmap.height} → ${newWidth}x${newHeight} (bilinear filter)")
        // IMPORTANT: 'true' parameter enables bilinear filtering for crisp text edges
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun preprocessForOcr(bitmap: Bitmap): Bitmap {
        Log.d(TAG, "Minimal preprocessing: ${bitmap.width}x${bitmap.height}, keeping original colors for ML Kit")
        return try {
            var result = bitmap

            // IMPORTANT: Keep original color image - ML Kit works BEST with color images!
            // Do NOT convert to grayscale or binarize - these destroy texture info

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

            Log.d(TAG, "Preprocessing finished (color preserved): ${result.width}x${result.height}")
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
     * Advanced binarization for OCR (ADAPTIVE)
     * Uses dynamic threshold based on image brightness
     * Handles different lighting conditions better than fixed threshold
     * Adaptive approach: threshold = average brightness ± adaptive offset
     */
    private fun binarizeForHandwriting(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Single-pass: calculate brightness stats
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalGray = 0L
        var minGray = 255
        var maxGray = 0

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            totalGray += gray
            minGray = minOf(minGray, gray)
            maxGray = maxOf(maxGray, gray)
        }

        val avgGray = (totalGray / pixels.size).toInt()
        val contrast = maxGray - minGray

        // Adaptive threshold: use average as base, adjust by contrast
        val threshold = when {
            contrast < 30 -> avgGray  // Low contrast: use average
            contrast > 150 -> (avgGray * 0.95f).toInt()  // High contrast: lower threshold slightly
            else -> (avgGray * 0.98f).toInt()  // Medium contrast: slight adjustment
        }.coerceIn(80, 170)

        // Apply threshold (binarize)
        val binarized = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            pixels[i] = if (gray < threshold) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }

        binarized.setPixels(pixels, 0, width, 0, 0, width, height)

        Log.d(TAG, "Adaptive binarized: threshold=$threshold (avg=$avgGray, contrast=$contrast)")
        return binarized
    }

    /**
     * Extract ROI (Region of Interest) from bitmap with custom dimensions + padding.
     * ML Kit needs whitespace around text to recognize it as text blocks
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

        // Add whitespace padding (IMPORTANT for ML Kit text recognition)
        val paddingPixels = (width * 0.05f).toInt().coerceIn(10, 50)  // 5% padding, min 10px, max 50px
        val paddedX = (x1 - paddingPixels).coerceAtLeast(0)
        val paddedY = (y1 - paddingPixels).coerceAtLeast(0)
        val paddedWidth = (width + 2 * paddingPixels).coerceAtMost(currentBitmap.width - paddedX)
        val paddedHeight = (height + 2 * paddingPixels).coerceAtMost(currentBitmap.height - paddedY)

        Log.d(TAG, "Final ROI with padding: x=$paddedX, y=$paddedY, w=$paddedWidth, h=$paddedHeight, padding=$paddingPixels")
        return Bitmap.createBitmap(currentBitmap, paddedX, paddedY, paddedWidth, paddedHeight)
    }
}
