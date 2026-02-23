package com.fittrack.app.data

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink

sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ModelDownloadManager(private val context: Context) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: Flow<DownloadState> = _downloadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: Flow<Float> = _downloadProgress.asStateFlow()

    private val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-3-270m-it/resolve/main/gemma3-270m-it-q8.task?download=true"
    private val MODEL_FILENAME = "gemma3-270m-it-q8.task"

    private val okHttpClient =
            OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.MINUTES) // Large file download
                    .build()

    fun getModelFile(): File {
        return File(context.filesDir, MODEL_FILENAME)
    }

    suspend fun isModelDownloaded(): Boolean =
            withContext(Dispatchers.IO) {
                val file = getModelFile()
                file.exists() &&
                        file.length() >
                                200_000_000L // Approx 300MB, so verify it's reasonably large
            }

    suspend fun downloadModelIfNeeded() =
            withContext(Dispatchers.IO) {
                if (isModelDownloaded()) {
                    _downloadState.value = DownloadState.Completed
                    _downloadProgress.value = 1f
                    return@withContext
                }

                _downloadState.value = DownloadState.Downloading
                _downloadProgress.value = 0f

                try {
                    val request = Request.Builder().url(MODEL_URL).build()
                    val response = okHttpClient.newCall(request).execute()

                    if (!response.isSuccessful) {
                        _downloadState.value =
                                DownloadState.Error(
                                        "Failed to download model: HTTP ${response.code}"
                                )
                        return@withContext
                    }

                    val body = response.body
                    if (body == null) {
                        _downloadState.value = DownloadState.Error("Empty response body")
                        return@withContext
                    }

                    val contentLength = body.contentLength()
                    val file = getModelFile()

                    // Write to a temporary file first so that an interrupted download doesn't
                    // appear as completed
                    val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

                    val source = body.source()
                    val sink = tempFile.sink().buffer()

                    var totalBytesRead = 0L
                    var lastProgressUpdate = 0L
                    val bufferSize = 8192L

                    while (true) {
                        val read = source.read(sink.buffer, bufferSize)
                        if (read == -1L) break

                        totalBytesRead += read
                        sink.emitCompleteSegments()

                        if (contentLength > 0) {
                            val currentProgress = System.currentTimeMillis()
                            // Update progress at most every 200ms
                            if (currentProgress - lastProgressUpdate > 200) {
                                _downloadProgress.value =
                                        totalBytesRead.toFloat() / contentLength.toFloat()
                                lastProgressUpdate = currentProgress
                            }
                        }
                    }

                    sink.close()
                    source.close()

                    // Rename temp file to actual file
                    if (tempFile.renameTo(file)) {
                        _downloadProgress.value = 1f
                        _downloadState.value = DownloadState.Completed
                        Log.d(
                                "ModelDownloadManager",
                                "Model download completed successfully at ${file.absolutePath}"
                        )
                    } else {
                        _downloadState.value = DownloadState.Error("Failed to save downloaded file")
                    }
                } catch (e: Exception) {
                    Log.e("ModelDownloadManager", "Error downloading model", e)
                    _downloadState.value =
                            DownloadState.Error(e.message ?: "Unknown download error")

                    // Clean up temp file if it exists
                    val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                }
            }
}
