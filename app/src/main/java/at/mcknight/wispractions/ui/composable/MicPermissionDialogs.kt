package at.mcknight.wispractions.ui.composable

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import at.mcknight.wispractions.DialogType
import at.mcknight.wispractions.DialogType.*

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
) {
    Log.i("MicPermissions", "dialogState: ${dialogState?.javaClass}")
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
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = { dismissHandler() },
                title = { Text(dialogState.title) },
                text = { Text(dialogState.msg) },
                confirmButton = {
                    TextButton(onClick = { launchSettingsActivity(context) }) {
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

/**
 * Launch to microphone settings page
 */
private fun launchSettingsActivity(context: Context) {
    Log.i("MicPermissions", "launchSettingsActivity()")
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    })
}
