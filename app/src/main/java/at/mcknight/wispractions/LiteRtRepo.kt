package at.mcknight.wispractions

import android.content.res.AssetManager
import android.provider.AlarmClock
import android.util.Log
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 *On-device LLM.  We use this to infer the user's intent when they issue a verbal command.  The idea
 * is that we take the raw transcribed text and turn it into a JSON string that can be deserialized
 * into an Android Intent.
 *
 * Emulator with Android API 37 required to initialize the [Engine] (maybe API 36 works?)
 * **Emulator with Android API 35 definitely does not work**, and will crash with SIGILL (signal 4,
 * illegal opcode) inside nativeCreateEngine in liblitertlm_jni.so.
 *
 * @param engine
 * @param assetManager the android [AssetManager]; we need this to copy the LLM model file to the
 * Android file system
 * @param modelAssetDirName the location of the model files in the Assets folder; in production, we
 * would not put the model files in the Assets folder; we would download the model files, either
 * with a hand-rolled downloader, or use something like Google Play Asset Delivery (PAD)
 * @param modelDir the location of the model files in the file system; this is where the
 *  *   model files need to be for the [Engine] to access them
 */
class LiteRtRepo(
    private val engine: Engine,
    private val assetManager: AssetManager,
    private val modelAssetDirName: String,
    private val modelDir: File
) {


    private val conversationConfig = ConversationConfig(
        systemInstruction = Contents.of(systemInstructions),
    )

    /**
     * Initialize the LiteRT-LM engine. We use Dispatchers.IO because initialization takes a long
     * time, up to 10 min (?)
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            Log.i("LiteRtRepo", "initialize() -- started")
            copyModelFromAssets(modelAssetDirName, assetManager, modelDir)
            Log.i("LiteRtRepo", "initialize() -- models copied")
            engine.initialize()
            Log.i("LiteRtRepo", "initialize() -- finished")
        }
    }

    fun prompt(prompt: String): String {
        AlarmClock.ACTION_SET_TIMER
        Log.i("LitRtRepo", "prompt: $prompt")
        engine.createConversation(conversationConfig).use { conversation ->
            val message = conversation.sendMessage(prompt)
            Log.i("LitRtRepo", "message: $message")
            return message.toString()
        }
    }

    companion object {
        val systemInstructions = """
                You are an intent parser. Given a short voice command, respond ONLY with valid JSON:
                {
                  "action": "ACTION_SET_TIMER",
                  "extras": {
                    "AlarmClock.EXTRA_LENGTH": <TimerDuration>,
                    "AlarmClock.EXTRA_MESSAGE": <TimerName>
                  }
                }
                Do not include the word "json" in the response.  It should be possible to pass the 
                response directly to a JSON parser without modifications.
                Omit AlarmClock.EXTRA_MESSAGE if no timer name is specified.
                Set TimerDuration to 60 if no duration is specified.
                If unknown, use action: "UNKNOWN".
                """.trimIndent()
    }
}


