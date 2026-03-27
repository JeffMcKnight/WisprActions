package at.mcknight.wispractions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.mcknight.wispractions.PermissionAction.DismissDialog
import at.mcknight.wispractions.PermissionAction.LaunchPermissionRequest
import at.mcknight.wispractions.PermissionAction.PermissionDenied
import at.mcknight.wispractions.PermissionAction.PermissionGranted
import at.mcknight.wispractions.PermissionAction.RequestMicPermission
import at.mcknight.wispractions.ui.composable.MicPermissionDialogs

/**
 * Handles microphone permission requests using Jetpack Compose dialogs.  Drives dialog visibility
 * via [dialogState], which we consume in the [MicPermissionDialogs] Composable.
 */
class MicrophonePermissionHandler() {

    var awaitingSettingsReturn by mutableStateOf(false)
        private set

    /**
     * The current dialog to display, or `null` if no dialog should be shown.
     */
    var dialogState by mutableStateOf<DialogType?>(null)
        private set

    /**
     * Handle the [PermissionAction]
     * @param action indicates what action to take
     */
    fun sendAction(action: PermissionAction) {
        when(action) {
            DismissDialog -> dismissDialog()
            LaunchPermissionRequest -> launchPermissionRequest()
            is PermissionDenied -> onPermissionDenied(action.permanentlyDenied)
            PermissionGranted -> onPermissionGranted()
            is RequestMicPermission -> requestMicPermission(action)
            PermissionAction.ReturnedFromSettings -> awaitingSettingsReturn = false
            PermissionAction.SettingsLaunched -> awaitingSettingsReturn = true
        }
    }

    /**
     * Entry point for requesting microphone permission.
     *
     * Checks current permission state and sets [dialogState] accordingly:
     * - Already granted: calls [onPermissionGranted] directly
     * - Previously denied: shows [DialogType.Rationale]
     * - First time or permanently denied: shows [DialogType.PrePrompt]
     */
    private fun requestMicPermission(action: RequestMicPermission) {
        when {
            action.isGranted -> onPermissionGranted()
            action.shouldShowRationale -> showRationaleDialog()
            else -> showPrePromptDialog()
        }
    }

    private fun showRationaleDialog() {
        dialogState = DialogType.Rationale()
    }

    private fun showPrePromptDialog() {
        dialogState = DialogType.PrePrompt()
    }

    /**
     * Dismisses the currently visible dialog by resetting [dialogState] to `null`.
     */
    private fun dismissDialog() {
        dialogState = null
    }

    /**
     * Dismisses the dialog and launches the OS microphone permission prompt.
     */
    private fun launchPermissionRequest() {
        dialogState = null
    }

    /**
     * TODO: call the success listener (or emit on a Flow?)
     */
    private fun onPermissionGranted() {
        // Start recording, update UI, etc.
    }

    /**
     * Show a dialog to indicate that permissions are required, if permissions request is permanently denied
     */
    private fun onPermissionDenied(permanentlyDenied: Boolean) {
        if (permanentlyDenied) {
            dialogState = DialogType.PermissionRequired()
        }
    }

}

/**
 * All the actions that can be handled by [MicrophonePermissionHandler]
 */
sealed class PermissionAction {
    data object DismissDialog : PermissionAction()

    /**
     * TODO: delete this
     */
    data object LaunchPermissionRequest : PermissionAction()
    data class PermissionDenied(val permanentlyDenied: Boolean) : PermissionAction()
    data object PermissionGranted : PermissionAction()
    data class RequestMicPermission(val shouldShowRationale: Boolean, val isGranted: Boolean) : PermissionAction()
    data object ReturnedFromSettings : PermissionAction()
    data object SettingsLaunched : PermissionAction()
}

/**
 * Represents the type of permission dialog to display.
 */
sealed class DialogType {
    /** Shown before the OS prompt, on first request. */
    data class PrePrompt(
        val title: String = "Microphone Access",
        val msg: String = "We need mic access to record voice memos. You'll see a system prompt next.",
        val confirmLabel: String = "Continue",
        val dismissLabel: String = "Not Now"
    ) : DialogType()

    /** Shown when the user previously denied the permission. */
    data class Rationale(
        val title: String = "Microphone Required",
        val msg: String = "Recording won't work without microphone access.",
        val confirmLabel: String = "Try Again",
        val dismissLabel: String = "Cancel"
    ) : DialogType()

    /** Shown when the user previously denied the permission. */
    data class PermissionRequired(
        val title: String = "Microphone Required",
        val msg: String = "Recording won't work without microphone access. After enabling on the app Settings page, use the back button or swipe up to return to Wispr.",
        val confirmLabel: String = "Open Settings",
        val dismissLabel: String = "Cancel"
    ) : DialogType()
}

