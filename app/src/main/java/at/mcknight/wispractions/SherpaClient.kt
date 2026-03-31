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
 *
 * @param assetManager
 * @param modelAssetDir the location of the model files in the Assets folder; in production, we
 * would not put the model files in the Assets folder; we would download the model files, either
 * with a hand-rolled downloader, or use something like Google Play Asset Delivery (PAD)
 * @param modelDestinationDir the location of the model files in the file system; this is where the
 *   model files need to be for the [OnlineRecognizer] to access them
 */
class SherpaClient(
    assetManager: AssetManager,
    modelAssetDir: String,
    modelDestinationDir: File
) {

    init {
        copyModelFromAssets(
            modelAssetDir,
            assetManager,
            modelDestinationDir
        )
    }
    // Build recognizer config
    val modelConfig = OnlineModelConfig(
        transducer = OnlineTransducerModelConfig(
            encoder = "${modelDestinationDir.path}/encoder-epoch-99-avg-1.onnx",
            decoder = "${modelDestinationDir.path}/decoder-epoch-99-avg-1.onnx",
            joiner = "${modelDestinationDir.path}/joiner-epoch-99-avg-1.onnx",
        ),
        tokens = "${modelDestinationDir.path}/tokens.txt",
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
     *  We have some defensive code in case we get an empty array of audio samples, which will
     *  cause a SIGABRT on the RenderThread (frame starvation).
     *
     * TODO: call [OnlineStream.inputFinished] and/or [OnlineStream.release] when recording stops
     *
     * @param audioSamples audio sample data as an array of Floats
     * @return the transcribed text or null if the [recognizer] has not detected the end of the spoken phrase
     */
    fun transcribe(audioSamples: FloatArray): String? {
        if (audioSamples.isEmpty()) return null

        stream.acceptWaveform(audioSamples, SAMPLE_RATE)
        while (recognizer.isReady(stream)) {
            recognizer.decode(stream)
        }
        if (!recognizer.isEndpoint(stream)) return null

        val result = recognizer.getResult(stream).text
        if (result.isNotEmpty()) {
            Log.i("SherpaClient", "result: $result")
        } else {
            Log.v("SherpaClient", "result: $result")
        }
        if (result == "") return null

        recognizer.reset(stream)
        return result
    }


}
    /**
     * Copies LLM model files from [assetPath] to [destDir], as necessary.
     *
     * @param assetPath the path to the file in the Assets directory
     * @param assetManager
     * @param destDir the directory in the file system to copy the LLM model files to
     */
    fun copyModelFromAssets(
        assetPath: String,
        assetManager: AssetManager,
        destDir: File
    ) {
        // create the destination directory if it doesn't exist
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        assetManager.list(assetPath)?.forEach { filename ->
            assetManager.open("$assetPath/$filename").use { input ->
                val destFile = File(destDir, filename)
                // Only copy the file if it does not exist yet
                if (!destFile.exists()) {
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

