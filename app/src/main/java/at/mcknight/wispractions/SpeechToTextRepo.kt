package at.mcknight.wispractions

import android.Manifest
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Controls microphone recording and LLM transcription of the audio recorded by the mic.
 */
class SpeechToTextRepo(
    private val sherpaClient: SherpaClient,
    private val audioRecorder: AudioRecorder
) {

    /**
     * Map audio samples flow to a transcribed String using the [SherpaClient]. We filter out nulls
     * because [SherpaClient.transcribe] will return null until it has made a successful transcription.
     *
     * We use [Dispatchers.Default] here because the decode call is blocking and CPU-heavy, so we
     * want to keep all that work on a background thread.
     */
    val transcript: Flow<String> = audioRecorder.audioChunks
        .map { sherpaClient.transcribe(it) }
        .flowOn(Dispatchers.Default) // do heavy transcription work on the Default Dispatcher
        .filterNotNull()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun toggle(): RecorderState {
        return audioRecorder.toggle()
    }

    fun stop() {
        audioRecorder.stop()
    }

}
