package at.mcknight.wispractions

import android.content.res.AssetManager
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
        Log.i("LitRtRepo", "prompt: $prompt")
        engine.createConversation(conversationConfig).use { conversation ->
            val message = conversation.sendMessage(prompt)
            Log.i("LitRtRepo", "message: $message")
            return message.toString().trimCruft()
        }
    }

    companion object {
        val systemInstructions = """
                You extract parameters from user input and return ONLY valid JSON.  Schema:
                {
                  "action": String,
                  "duration": Int,
                  "name": String,
                  "timeUnits": String
                }
                Return ONLY the JSON object. No explanation, no markdown fences, no preamble.
                Set `action`: "android.intent.action.SET_TIMER" if user input includes the word timer or alarm 
                If unknown, set `action`: "UNKNOWN".
                Set `duration` property to first number in the user input
                Set `duration` property to 60 if no duration is specified.
                Set `timeUnits` property to day, hour, minute, or second. 
                Set `name` property to empty string if no timer name is specified.
                Examples:
                User: "Start a timer for 55 seconds"
                Output: {
                  "action": "android.intent.action.SET_TIMER",
                  "duration": "55",
                  "name": "",
                  "timeUnits": "second"
                }
                User: "Set a timer for forty seven minutes called Laundry"
                Output: {
                  "action": "android.intent.action.SET_TIMER",
                  "duration": "47",
                  "name": "Laundry",
                  "timeUnits": "minute"
                }
                User: "Start a fourteen hour timer called Dishwasher"
                Output: {
                  "action": "android.intent.action.SET_TIMER",
                  "duration": "14",
                  "name": "Dishwasher",
                  "timeUnits": "hour"
                }
                User: "Set a four day timer called Lasagna"
                Output: {
                  "action": "android.intent.action.SET_TIMER",
                  "duration": "4",
                  "name": "Lasagna",
                  "timeUnits": "day"
                }
                """.trimIndent()
    }
}

/**
 * Trim leading and trailing backticks and "json".  The LLM really wants to include this extra cruft
 * even though the system instructions explicitly tell not to do so.
 */
private fun String.trimCruft(): String {
    val cruftRemoved = this
        .trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("\n```")
        .trim()
    return cruftRemoved
}

