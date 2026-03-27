package at.mcknight.wispractions.ui.composable

import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import at.mcknight.wispractions.DialogType
import at.mcknight.wispractions.DialogType.PermissionRequired
import at.mcknight.wispractions.DialogType.PrePrompt
import at.mcknight.wispractions.DialogType.Rationale
import at.mcknight.wispractions.LOG_TAG

/**
 * Renders the appropriate microphone permission dialog based on [DialogType].
 *
 * Place this at the top level of any Composable screen that triggers mic permission requests.
 *
 * @param dialogState Drives dialog visibility.
 * @param dismissHandler sends UI events back to the ViewModel
 */
@Composable
fun MicPermissionDialogs(
    dialogState: DialogType?,
    confirmHandler: () -> Unit,
    dismissHandler: () -> Unit,
    permissionsRequiredHandler: () -> Unit,
) {
    Log.i(LOG_TAG, "dialogState: ${dialogState?.javaClass}")
    when (dialogState) {
        is PrePrompt -> {
            AlertDialog(
                onDismissRequest = { dismissHandler() },
                title = { Text(dialogState.title) },
                text = { Text(dialogState.msg) },
                confirmButton = {
                    TextButton(onClick = { confirmHandler() }) {
                        Text(dialogState.confirmLabel)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dismissHandler() }) {
                        Text(dialogState.dismissLabel)
                    }
                }
            )
        }
        is Rationale -> {
            AlertDialog(
                onDismissRequest = { dismissHandler() },
                title = { Text(dialogState.title) },
                text = { Text(dialogState.msg) },
                confirmButton = {
                    TextButton(onClick = { confirmHandler() }) {
                        Text(dialogState.confirmLabel)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dismissHandler() }) {
                        Text(dialogState.dismissLabel)
                    }
                }
            )
        }
        is PermissionRequired -> {
            AlertDialog(
                onDismissRequest = { dismissHandler() },
                title = { Text(dialogState.title) },
                text = { Text(dialogState.msg) },
                confirmButton = {
                    TextButton(onClick = { permissionsRequiredHandler() }) {
                        Text(dialogState.confirmLabel)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dismissHandler() }) {
                        Text(dialogState.dismissLabel)
                    }
                }
            )

        }
        null -> Unit
    }
}

