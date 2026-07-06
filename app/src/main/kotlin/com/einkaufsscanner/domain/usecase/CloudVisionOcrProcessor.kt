package com.einkaufsscanner.domain.usecase

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.json.JSONObject
import org.json.JSONArray

/**
 * Google Cloud Vision API integration for superior handwriting recognition
 * Uses DOCUMENT_TEXT_DETECTION which handles handwritten text much better than ML Kit
 *
 * To use:
 * 1. Get a free Google Cloud Vision API key from https://console.cloud.google.com/
 * 2. Set environment variable: GOOGLE_CLOUD_API_KEY=your-key-here
 * 3. Or add to local.properties: vision.api.key=your-key-here
 */
object CloudVisionOcrProcessor {
    private const val TAG = "CloudVisionOCR"
    private const val VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate"

    /**
     * Recognize text from bitmap using Google Cloud Vision API
     * Better for handwritten text than ML Kit
     */
    suspend fun recognizeTextFromCloud(bitmap: Bitmap, apiKey: String): String = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            try {
                if (apiKey.isEmpty()) {
                    val error = "Google Cloud Vision API key not provided"
                    android.util.Log.e(TAG, error)
                    continuation.resumeWithException(
                        IllegalStateException(error)
                    )
                    return@suspendCancellableCoroutine
                }

                android.util.Log.d(TAG, "Cloud Vision API call with key length=${apiKey.length}")

                // Convert bitmap to base64
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val imageBytes = stream.toByteArray()
                val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                // Build request JSON
                val requestJSON = JSONObject().apply {
                    put("requests", JSONArray().apply {
                        put(JSONObject().apply {
                            put("image", JSONObject().apply {
                                put("content", imageBase64)
                            })
                            put("features", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "DOCUMENT_TEXT_DETECTION")  // Best for handwriting
                                    put("maxResults", 1)
                                })
                            })
                        })
                    })
                }

                // Make HTTP request
                val url = URL("$VISION_API_URL?key=$apiKey")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Send request
                val outputStream = connection.outputStream
                outputStream.write(requestJSON.toString().toByteArray())
                outputStream.close()

                // Read response
                val responseCode = connection.responseCode
                android.util.Log.d(TAG, "Cloud Vision API response code: $responseCode")

                if (responseCode != 200) {
                    val errorStream = connection.errorStream.bufferedReader().readText()
                    android.util.Log.e(TAG, "Vision API error: $responseCode - $errorStream")
                    throw Exception("Vision API error: $responseCode - $errorStream")
                }

                val responseText = connection.inputStream.bufferedReader().readText()
                android.util.Log.d(TAG, "Vision API response length: ${responseText.length}")

                val responseJSON = JSONObject(responseText)

                // Extract text from response
                val responses = responseJSON.getJSONArray("responses")
                val response = responses.getJSONObject(0)

                val recognizedText = if (response.has("fullTextAnnotation")) {
                    response.getJSONObject("fullTextAnnotation").getString("text")
                } else {
                    ""
                }

                android.util.Log.d(TAG, "Cloud Vision recognized text length: ${recognizedText.length}")
                continuation.resume(recognizedText)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }
}
