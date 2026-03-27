package at.mcknight.wispractions

import androidx.lifecycle.ViewModel
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


    fun sendAction(action: PermissionAction) {
        permissionHandler.sendAction(action)
    }

}

data class MainUiState(
    val name: String = "Google"
)
