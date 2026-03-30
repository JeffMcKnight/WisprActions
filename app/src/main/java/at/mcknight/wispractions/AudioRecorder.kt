package at.mcknight.wispractions

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRecord.READ_BLOCKING
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioRecorder(private val scope: CoroutineScope) {

    companion object {
        /** 16kHz — required for STT (Sherpa) */
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _audioChunks = MutableSharedFlow<FloatArray>()
    val audioChunks: SharedFlow<FloatArray> = _audioChunks

    /**
     * TODO: add a StateFlow to keep track of recording state
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun toggle() {
        if (audioRecord != null) stop() else start()
    }

    /**
     * Start recording from the microphone. We launch the [CoroutineScope] with the [Dispatchers.IO]
     * context since [AudioRecord.read] is a blocking I/O operation.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        val bufferSize: Int = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        require(bufferSize != AudioRecord.ERROR_BAD_VALUE) { "Invalid AudioRecord params" }

        Log.i("AudioRecorder", "bufferSize: $bufferSize")
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AudioFormat.ENCODING_PCM_FLOAT,
            bufferSize * 4   // larger buffer reduces underrun risk
        ).also { record ->
            require(record.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord init failed" }
            record.startRecording()
        }

        recordingJob = scope.launch(Dispatchers.IO) {
            val buffer = FloatArray(bufferSize)
            while (isActive) {
                val samplesRead = audioRecord?.read(buffer, 0, buffer.size,  READ_BLOCKING) ?: break
                val maxAmplitude = buffer.take(samplesRead).maxOrNull() ?: 0
                Log.d("AudioRecorder", "bytes=$samplesRead maxAmplitude=$maxAmplitude")
                if (samplesRead > 0) {
                    _audioChunks.emit(buffer.copyOf(samplesRead))
                }
            }
        }
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}