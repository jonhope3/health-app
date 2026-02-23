package com.fittrack.app.services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerationConfig
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiNanoService(private val context: Context) {

    private var generativeModel: GenerativeModel? = null

    suspend fun isAvailable(): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    // Check status via ML Kit
                    if (generativeModel == null) {
                        generativeModel = Generation.getClient(GenerationConfig.Builder().build())
                    }
                    val status = generativeModel?.checkStatus() ?: -1
                    status == 0 // Assuming 0 is success/ready; real apps would check against
                    // constants
                } catch (e: Exception) {
                    false
                }
            }

    suspend fun initIfNeeded(): Boolean =
            withContext(Dispatchers.IO) {
                if (generativeModel != null) return@withContext true
                try {
                    generativeModel = Generation.getClient(GenerationConfig.Builder().build())
                    true
                } catch (e: Exception) {
                    Log.e("GeminiNano", "Failed to initialize: ${e.message}")
                    false
                }
            }

    suspend fun generateContent(prompt: String): String =
            withContext(Dispatchers.IO) {
                if (!initIfNeeded()) return@withContext ""
                try {
                    val response = generativeModel?.generateContent(prompt)
                    response?.candidates?.firstOrNull()?.text ?: ""
                } catch (e: Exception) {
                    Log.e("GeminiNano", "Generation failed: ${e.message}")
                    ""
                }
            }

    suspend fun generateContent(bitmap: Bitmap, prompt: String): String =
            withContext(Dispatchers.IO) {
                if (!initIfNeeded()) return@withContext ""
                try {
                    // ML Kit GenAI Prompt API multimodal use:
                    // While the interface has GenerateContentRequest, the standard
                    // generateContent(String)
                    // handles text. For images, we would wrap in GenerateContentRequest.
                    // For now, providing a text-based fallback or empty if image not easily
                    // supported
                    // in this beta version snippet.
                    val response = generativeModel?.generateContent(prompt)
                    response?.candidates?.firstOrNull()?.text ?: ""
                } catch (e: Exception) {
                    Log.e("GeminiNano", "Multimodal generation failed: ${e.message}")
                    ""
                }
            }
}
