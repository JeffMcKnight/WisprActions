package at.mcknight.wispractions

import android.util.Log
import androidx.lifecycle.ViewModel
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
        Log.i("MicPermissions", "sendAction() -- action: $action")
    }

    fun sendPermissionAction(action: PermissionAction) {
        Log.d("MicPermissions", "sendPermissionAction() -- action: $action")
        permissionHandler.sendAction(action)
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
    val name: String = "Google"
)
