package com.fittrack.app.services

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class GeminiNanoService {

    private var generativeModel: GenerativeModel? = null

    suspend fun checkAvailability(): String = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: Generation.getClient().also { generativeModel = it }
            when (model.checkStatus()) {
                FeatureStatus.AVAILABLE -> "available"
                FeatureStatus.DOWNLOADABLE -> "downloadable"
                FeatureStatus.DOWNLOADING -> "downloading"
                FeatureStatus.UNAVAILABLE -> "unavailable"
                else -> "unavailable"
            }
        } catch (e: Exception) {
            Log.w("GeminiNano", "checkAvailability failed: ${e.message}")
            "unavailable"
        }
    }

    suspend fun downloadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: Generation.getClient().also { generativeModel = it }
            var success = false
            model.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted ->
                        Log.d("GeminiNano", "Download started")
                    is DownloadStatus.DownloadProgress ->
                        Log.d("GeminiNano", "Download progress: ${status.totalBytesDownloaded} bytes")
                    is DownloadStatus.DownloadCompleted -> {
                        Log.d("GeminiNano", "Download completed")
                        success = true
                    }
                    is DownloadStatus.DownloadFailed ->
                        Log.e("GeminiNano", "Download failed: ${status.e.message}")
                }
            }
            success
        } catch (e: Exception) {
            Log.e("GeminiNano", "downloadModel failed: ${e.message}")
            false
        }
    }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: Generation.getClient().also { generativeModel = it }
            val response = model.generateContent(prompt)
            response.candidates.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            Log.e("GeminiNano", "generateContent failed: ${e.message}")
            ""
        }
    }

    suspend fun generateContent(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: Generation.getClient().also { generativeModel = it }
            val request = generateContentRequest(ImagePart(bitmap), TextPart(prompt)) {}
            val response = model.generateContent(request)
            response.candidates.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            Log.e("GeminiNano", "generateContent (image) failed: ${e.message}")
            ""
        }
    }

    suspend fun initIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        try {
            val status = checkAvailability()
            Log.d("GeminiNano", "Status: $status")
            if (status == "available") {
                runCatching { (generativeModel ?: Generation.getClient().also { generativeModel = it }).warmup() }
                return@withContext true
            }
            if (status == "downloadable" || status == "downloading") {
                Log.d("GeminiNano", "Triggering download...")
                if (downloadModel()) {
                    val post = checkAvailability()
                    Log.d("GeminiNano", "Post-download: $post")
                    if (post == "available") {
                        runCatching { (generativeModel ?: Generation.getClient().also { generativeModel = it }).warmup() }
                        return@withContext true
                    }
                }
                repeat(12) {
                    delay(5000)
                    val polled = checkAvailability()
                    Log.d("GeminiNano", "Polling: $polled")
                    if (polled == "available") return@withContext true
                }
            }
            Log.w("GeminiNano", "Could not init, final status: $status")
            false
        } catch (e: Exception) {
            Log.e("GeminiNano", "initIfNeeded failed: ${e.message}", e)
            false
        }
    }
}
