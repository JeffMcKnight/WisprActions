package at.mcknight.wispractions

import android.content.res.AssetManager
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 *On-device LLM
 *
 * Emulator with Android API 37 required to initialize the [Engine] (maybe API 36 works?)
 * Emulator with Android API 35 definitely does not work, and will crash with SIGILL (signal 4,
 * illegal opcode) inside nativeCreateEngine in liblitertlm_jni.so.
 *
 * @param engine
 * @param assetManager the android [AssetManager]; we need this to copy the LLM model file to the
 * Android file system
 * @param modelAssetDirName the location of the LLM files in the Assets folder in the Android app package
 * @param modelDir the location of the model files in the file system; this is where the
 *  *   model files need to be for the [Engine] to access them
 */
class LiteRtRepo(
    private val engine: Engine,
    private val assetManager: AssetManager,
    private val modelAssetDirName: String,
    private val modelDir: File
) {

    /**
     * Initialize the LiteRT-LM engine. We use Dispatchers.IO because initialization takes a long
     * time, up to 10 min (?)
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            Log.d("LiteRtRepo", "initialize() started")
            copyModelFromAssets(modelAssetDirName, assetManager, modelDir)
            engine.initialize()
            Log.d("LiteRtRepo", "initialize() finished")
        }
    }
}
