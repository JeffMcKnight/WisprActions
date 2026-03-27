package at.mcknight.wispractions.ui.composable

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.mcknight.wispractions.DialogType
import at.mcknight.wispractions.MainUiState
import at.mcknight.wispractions.ui.theme.WisprActionsTheme

/**
 * The root Composable for the main Activity
 */
@Composable
fun MainUi(
    uiState: MainUiState,
    dialogState: DialogType?,
    clickHandler: () -> Unit,
    confirmHandler: () -> Unit,
    dismissHandler: () -> Unit,
    permissionsRequiredHandler: () -> Unit,
) {
    WisprActionsTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()) { innerPadding ->
            MicrophoneButton(
                name = uiState.name,
                modifier = Modifier.padding(innerPadding),
                clickHandler = { clickHandler() }
            )
            MicPermissionDialogs(
                dialogState = dialogState,
                confirmHandler = { confirmHandler() },
                dismissHandler = { dismissHandler() },
                permissionsRequiredHandler = { permissionsRequiredHandler() }
            )
        }
    }
}
