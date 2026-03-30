package at.mcknight.wispractions

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.mcknight.wispractions.PermissionAction.DismissDialog
import at.mcknight.wispractions.PermissionAction.RequestMicPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/**
 *
 */
class MainViewModel(
    private val permissionHandler: MicrophonePermissionHandler,
    private val promptRepo: PromptRepo,
    private val speechToTextRepo: SpeechToTextRepo,
) : ViewModel() {

    val dialogState get() = permissionHandler.dialogState

    private val _transcript = speechToTextRepo.transcript.shareIn(viewModelScope, Lazily)
    val transcript: Flow<String> = _transcript

    private val _intentFlow = _transcript.map { promptRepo.inferIntent(it) }
    val intentFlow: Flow<Intent> = _intentFlow

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

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
        Log.i(LOG_TAG, "sendAction() -- action: $action")
        speechToTextRepo.toggle()
    }

    fun sendPermissionAction(action: PermissionAction) {
        Log.d(LOG_TAG, "sendPermissionAction() -- action: $action")
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
    val name: String = "Press to Talk"
)
