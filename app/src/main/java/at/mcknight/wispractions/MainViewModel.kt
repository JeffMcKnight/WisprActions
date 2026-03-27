package at.mcknight.wispractions

import android.util.Log
import androidx.lifecycle.ViewModel
import at.mcknight.wispractions.PermissionAction.DismissDialog
import at.mcknight.wispractions.PermissionAction.RequestMicPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 *
 */
class MainViewModel(private val permissionHandler: MicrophonePermissionHandler) : ViewModel() {

    val dialogState get() = permissionHandler.dialogState

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun sendAction(action: MicClickAction) {
        if (!action.isGranted) {
            sendPermissionAction(action.toRequestMicPermission())
            return
        }
        sendPermissionAction(DismissDialog)
        Log.i(LOG_TAG, "sendAction() -- action: $action")
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
    val name: String = "Hold to Talk"
)
