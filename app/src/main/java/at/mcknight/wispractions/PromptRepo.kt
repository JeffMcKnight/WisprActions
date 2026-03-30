package at.mcknight.wispractions

import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.GenerativeModel

/**
 * Use this repo to send prompts to the ML Kit LLM
 */
class PromptRepo(val generativeModel: GenerativeModel) {

    private var modelDownloaded: Boolean = false

    suspend fun inferIntent(prompt: String): Intent {
        // Always check availability first
        val status = generativeModel.checkStatus()
        Log.i("PromptRepo", "status: $status")
        when (status) {
            FeatureStatus.UNAVAILABLE -> {
                Log.w(TAG,"Gemini Nano not supported on this device or device hasn't fetched the latest configuration to support it")
            }
            FeatureStatus.DOWNLOADABLE -> {
                // Gemini Nano can be downloaded on this device, but is not currently downloaded
                generativeModel.download().collect { status ->
                    when (status) {
                        is DownloadStatus.DownloadStarted ->
                            Log.d(TAG, "starting download for Gemini Nano")

                        is DownloadStatus.DownloadProgress ->
                            Log.d(TAG, "Nano ${status.totalBytesDownloaded} bytes downloaded")

                        DownloadStatus.DownloadCompleted -> {
                            Log.d(TAG, "Gemini Nano download complete")
                            modelDownloaded = true
                        }

                        is DownloadStatus.DownloadFailed -> {
                            Log.e(TAG, "Nano download failed ${status.e.message}")
                        }
                    }
                }
            }

            FeatureStatus.DOWNLOADING -> {
                // Gemini Nano currently being downloaded
            }

            FeatureStatus.AVAILABLE -> {
                // Gemini Nano currently downloaded and available to use on this device
            }
        }
        // Warmup (optional but recommended)
        generativeModel.warmup()

        val response = generativeModel.generateContent("$PROMPT_INSTRUCTIONS $prompt")

        // Streaming inference
//        var fullResponse = ""
//        generativeModel.generateContentStream("$PROMPT_INSTRUCTIONS $prompt").collect { chunk ->
//            val newChunkReceived = chunk.candidates[0].text
//            fullResponse += newChunkReceived
//            Log.i(TAG, fullResponse)
//        }
        return response.toIntent()
    }

    companion object {
        const val TAG = "PromptRepo"
        const val PROMPT_INSTRUCTIONS = "parse the text, identify which action the user is requesting, and generate an Android Intent, along with its parameters, serialized as JSON: "
    }
}

private fun GenerateContentResponse.toIntent(): Intent {
    return Intent(AlarmClock.ACTION_SET_TIMER).apply {
        putExtra(AlarmClock.EXTRA_LENGTH, 60)
    }
}
