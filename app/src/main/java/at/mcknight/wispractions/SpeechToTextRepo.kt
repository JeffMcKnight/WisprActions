package at.mcknight.wispractions

import android.Manifest
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class SpeechToTextRepo(
    private val sherpaClient: SherpaClient,
    private val audioRecorder: AudioRecorder
) {

    /**
     * Map audio samples flow to a transcribed String using the [SherpaClient]. We filter out nulls
     * because [SherpaClient.transcribe] will return null until it has made a successful transcription
     */
    val transcript: Flow<String> = audioRecorder.audioChunks
        .map { sherpaClient.transcribe(it) }
        .filterNotNull()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun toggle() {
        audioRecorder.toggle()
    }

}