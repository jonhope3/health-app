package com.fittrack.app.services

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaPipeLLMService(private val context: Context, private val modelPath: String) {

    private var llmInference: LlmInference? = null

    suspend fun initialize(): Boolean =
            withContext(Dispatchers.IO) {
                if (llmInference != null) return@withContext true

                try {
                    Log.d("MediaPipeLLM", "Initializing LlmInference with model at $modelPath")
                    val options =
                            LlmInference.LlmInferenceOptions.builder()
                                    .setModelPath(modelPath)
                                    .setMaxTokens(256)
                                    .build()

                    llmInference = LlmInference.createFromOptions(context, options)
                    Log.d("MediaPipeLLM", "Initialization successful")
                    true
                } catch (e: Exception) {
                    Log.e("MediaPipeLLM", "Failed to initialize LlmInference: ${e.message}", e)
                    false
                }
            }

    suspend fun generateContent(prompt: String): String =
            withContext(Dispatchers.IO) {
                try {
                    val inference = llmInference
                    if (inference == null) {
                        Log.e("MediaPipeLLM", "LlmInference not initialized")
                        return@withContext ""
                    }

                    // A typical Gemma instruction format prompt wrap.
                    // We use `<start_of_turn>user\nPROMPT<end_of_turn>\n<start_of_turn>model\n`
                    val formattedPrompt =
                            "<start_of_turn>user\n${prompt.trim()}<end_of_turn>\n<start_of_turn>model\n"

                    Log.d("MediaPipeLLM", "Generating response...")
                    val response = inference.generateResponse(formattedPrompt)
                    response ?: ""
                } catch (e: Exception) {
                    Log.e("MediaPipeLLM", "Error generating response: ${e.message}", e)
                    ""
                }
            }

    suspend fun tryClosing() =
            withContext(Dispatchers.IO) {
                try {
                    llmInference?.close()
                    llmInference = null
                } catch (e: Exception) {
                    Log.e("MediaPipeLLM", "Error closing inference: ${e.message}")
                }
            }
}
