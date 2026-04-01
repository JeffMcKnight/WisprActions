package at.mcknight.wispractions

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.mcknight.wispractions.PermissionAction.DismissDialog
import at.mcknight.wispractions.PermissionAction.RequestMicPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull

/**
 *
 */
class MainViewModel(
    private val permissionHandler: MicrophonePermissionHandler,
    private val liteRtRepo: LiteRtRepo,
    private val speechToTextRepo: SpeechToTextRepo
) : ViewModel() {

    val dialogState get() = permissionHandler.dialogState

    /** Pass-through flow to emit the Intent to launch in the [MainActivity] */
    private val _intentFlow = MutableSharedFlow<Intent>()
    val intentFlow: Flow<Intent> = _intentFlow

    /**
     * A Flow that emits the text transcribed in the [SpeechToTextRepo]; we make convert it to a
     * [SharedFlow] here so it can be shared between the UI and the LiteRT LLM
     */
    private val _transcript: SharedFlow<String> = speechToTextRepo.transcript.shareIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val transcript: SharedFlow<String> = _transcript
        .map { "${it.lowercase()}..." }
        .shareIn(viewModelScope, Lazily)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * Initialize the LiteRT engine, and then start collecting on the transcribed text.  We collect
     * and re-emit here because we need to wait until the LiteRT engine has finished initializing
     * before we start using it.
     *
     * Converts the command transcribed by Sherpa-ONNX in the [SpeechToTextRepo] into an Android
     * [Intent] that can be launched in the [MainActivity].  There are two steps:
     * 1. Feed the raw command to the LiteRT to convert to a JSON blob with the command action and parameters, and
     * 2. Deserialize the JSON into an [Intent] that can be executed
     */
    init {
        viewModelScope.launch {
            liteRtRepo.initialize()
            _transcript
                .map { liteRtRepo.prompt(it) }
                .flowOn(Dispatchers.Default)
                .map { it.toIntent() }
                .collect { _intentFlow.emit(it) }
        }
    }

    /**
     * Toggle speech-to-text if we have Record permissions.  We suppress the lint warning because
     * we explicitly check if permissions have been granted here. Every method from
     * [SpeechToTextRepo.toggle] on down is marked with the RequiresPermission annotation so we don't
     * inadvertently try to record when we don't have permission.
     */
    @SuppressLint("MissingPermission")
    fun sendAction(action: MicClickAction) {
        if (!action.isGranted) {
            sendPermissionAction(action.toRequestMicPermission())
            return
        }
        sendPermissionAction(DismissDialog)
        val recorderState = speechToTextRepo.toggle()
        viewModelScope.launch{ _uiState.emit(MainUiState(recorderState.toMicLabel())) }
    }


    fun sendPermissionAction(action: PermissionAction) {
        permissionHandler.sendAction(action)
    }

    /**
     * We call this to send a [MicClickAction] action for the user when they return from the
     * Settings page. [MicrophonePermissionHandler.awaitingSettingsReturn] lets us check that
     * we are returning from the settings page, and not just getting an onResume from an unrelated
     * lifecycle event.
     */
    fun handleResume(micClickAction: MicClickAction) {
        Log.i(LOG_TAG, "handleResume() -- ${permissionHandler.awaitingSettingsReturn}")
        if (!permissionHandler.awaitingSettingsReturn) return

        permissionHandler.sendAction(PermissionAction.ReturnedFromSettings)
        sendAction(micClickAction)
    }

    fun stopRecording() {
        speechToTextRepo.stop()
    }

}

/**
 * Generate a [RequestMicPermission] from the [MicClickAction] properties. We use this to request
 * record permissions if needed when user clicks the mic button.
 */
private fun MicClickAction.toRequestMicPermission(): RequestMicPermission {
    return RequestMicPermission(shouldShowRationale, isGranted)
}

data class MicClickAction(val shouldShowRationale: Boolean, val isGranted: Boolean)

data class MainUiState(
    val name: String = "Tap to Talk"
)

/**
 * Deserialize the JSON string into an [Intent].
 * If the extra parameter value is a String, also check if it's actually an integer, since the LLM
 * will sometimes pass the timer duration as a String instead of an integer.
 */
private fun String.toIntent(): Intent {
    Log.i("toIntent()",this)
    val (action, extras) = Json.decodeFromString<IntentData>(this)
    return Intent(action).apply {
        extras.forEach { (key, value): Map.Entry<String, JsonElement> ->
            when (value) {
                is JsonObject -> { /* value is JsonObject, access via value["key"] */ }
                is JsonArray -> { /* value is JsonArray, iterate value */ }
                is JsonPrimitive -> {
                    when {
                        value is JsonNull -> Unit
                        value.isString              -> putExtra(key, (value.intOrNull ?: value.content))
                        value.booleanOrNull != null -> putExtra(key, value.boolean)
                        value.intOrNull != null     -> putExtra(key, value.int)
                        value.doubleOrNull != null  -> putExtra(key, value.double)
                        else -> Log.w("toIntent()", "unexpected value type: ${value.content}")
                    }
                }
            }

        }
    }
}

/**
 * Tell the user what to do, or if we're listening
 */
private fun RecorderState.toMicLabel(): String {
    return when(this) {
        RecorderState.STARTED -> "Listening..."
        RecorderState.STOPPED -> "Tap to Talk"
    }
}
