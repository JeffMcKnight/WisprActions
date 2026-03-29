package at.mcknight.wispractions

import android.content.res.AssetManager
import android.util.Log
import at.mcknight.wispractions.AudioRecorder.Companion.SAMPLE_RATE
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.io.File


/**
 * TODO: inject [OnlineRecognizer] instead of building it here
 */
class SherpaClient(
    private val assetManager: AssetManager,
    private val filesDir: File,
    private val modelDirName: String
) {
    private val audioBuffer = mutableListOf<Float>()
    private val MIN_SAMPLES = 8000 // 500ms @ 16kHz

    val modelDir: File = copyModelFromAssets()

    // Build recognizer config
    val modelConfig = OnlineModelConfig(
        transducer = OnlineTransducerModelConfig(
            encoder = "${modelDir.path}/encoder-epoch-99-avg-1.onnx",
            decoder = "${modelDir.path}/decoder-epoch-99-avg-1.onnx",
            joiner = "${modelDir.path}/joiner-epoch-99-avg-1.onnx",
        ),
        tokens = "${modelDir.path}/tokens.txt",
        numThreads = 2,
        debug = false,
        provider = "cpu" // Use "nnapi" for hardware acceleration
    )

    val recognizerConfig = OnlineRecognizerConfig(
        modelConfig = modelConfig,
        endpointConfig = EndpointConfig(), // VAD / silence detection
        enableEndpoint = true,
    )

    val recognizer = OnlineRecognizer(config = recognizerConfig)
    val stream: OnlineStream = recognizer.createStream()

    /**
     * Pass audio samples to the [stream] and decode to text using the [OnlineRecognizer].
     *
     * @param audioSamples audio sample data as an array of Floats
     * @return the transcribed text or null if the [recognizer] has not reached the endpoint
     */
    fun transcribe(audioSamples: FloatArray): String? {
        audioBuffer.addAll(audioSamples.toList())
        if (audioBuffer.size < MIN_SAMPLES) return null

        Log.i("SherpaClient", "audioBuffer.size: ${audioBuffer.size}")
        stream.acceptWaveform(audioBuffer.toFloatArray(), SAMPLE_RATE)
        recognizer.decode(stream)
        if (!recognizer.isEndpoint(stream)) return null

        val result = recognizer.getResult(stream).text
        Log.i("SherpaClient", "result: $result")
        recognizer.reset(stream)
        audioBuffer.clear() // or use a sliding window
        return result
    }

    /**
     * Copies a sherpa-onnx model from assets to filesDir on first run.
     * Returns the directory in filesDir containing the model files.
     */
    private fun copyModelFromAssets(): File {
        val destDir = File(filesDir, modelDirName)
        if (destDir.exists()) return destDir  // already copied

        destDir.mkdirs()

        assetManager.list(modelDirName)?.forEach { filename ->
            assetManager.open("$modelDirName/$filename").use { input ->
                File(destDir, filename).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return destDir
    }

}
