package com.einkaufsscanner.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class DigitClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var isInitialized = false

    fun initialize() {
        try {
            val modelBuffer = loadModelFile("digit_classifier.tflite")
            interpreter = Interpreter(modelBuffer)
            isInitialized = true
            Log.d("DigitClassifier", "✅ TFLite MNIST model loaded successfully (294 KB)")
        } catch (e: Exception) {
            Log.w("DigitClassifier", "⚠️ TFLite model not found, will use fallback", e)
        }
    }

    fun classifyDigit(bitmap: Bitmap): String? {
        if (!isInitialized || interpreter == null) {
            Log.w("DigitClassifier", "Model not initialized")
            return null
        }

        return try {
            // Preprocess: resize to 28x28 and normalize (0-1)
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(28, 28, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            var tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // Run inference
            val outputArray = Array(1) { FloatArray(10) }
            interpreter?.run(tensorImage.buffer, outputArray)

            // Get digit with highest confidence
            val results = outputArray[0]
            val digit = results.indices.maxByOrNull { results[it] } ?: -1
            val confidence = results[digit]

            Log.d("DigitClassifier", "🖊️  TFLite classified: Digit=$digit, Confidence=${"%.2f".format(confidence)}")

            // Return if confidence is reasonable (>50%)
            if (confidence > 0.5f) {
                Log.d("DigitClassifier", "✅ Confidence OK (${(confidence*100).toInt()}%), returning digit: $digit")
                digit.toString()
            } else {
                Log.d("DigitClassifier", "⚠️ Confidence too low (${(confidence*100).toInt()}%), skipping")
                null
            }
        } catch (e: Exception) {
            Log.e("DigitClassifier", "Error classifying digit", e)
            null
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = assetFileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
        inputStream.close()
        return modelBuffer
    }

    fun close() {
        interpreter?.close()
        isInitialized = false
    }
}
