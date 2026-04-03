package at.mcknight.wispractions

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.AlarmClock
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

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
                .filterNotNull()
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
        viewModelScope.launch{ _uiState.emit(MainUiState("Tap to Talk")) }
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
 *
 * @return a timer [Intent] that is ready to launch, or null if the action is wrong
 */
private fun String.toIntent(): Intent? {
    Log.i("toIntent()",this)
    val intentData = Json.decodeFromString<IntentData>(this)
    if (intentData.action != AlarmClock.ACTION_SET_TIMER) return null
    return Intent(intentData.action).apply {
        putExtra(AlarmClock.EXTRA_MESSAGE, intentData.name)
        putExtra(AlarmClock.EXTRA_LENGTH, intentData.toLength())
    }
}

/**
 * Convert all supported time units (days, hours, and minutes) to seconds, if necessary. If we
 * cannot identify the time units, just return the default timer duration (60 sec)
 */
private fun IntentData.toLength(): Int {
    return when(timeUnits.lowercase()) {
        "day", "days"  -> duration * 60 * 60 * 24
        "minute", "minutes"  -> duration * 60
        "hour", "hours"  -> duration * 60 * 60
        "second", "seconds"  -> duration
        else -> 60
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
