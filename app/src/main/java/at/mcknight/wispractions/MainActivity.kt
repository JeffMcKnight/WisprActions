package at.mcknight.wispractions

import android.Manifest.permission.RECORD_AUDIO
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import at.mcknight.wispractions.PermissionAction.DismissDialog
import at.mcknight.wispractions.PermissionAction.PermissionDenied
import at.mcknight.wispractions.ui.composable.MainUi
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

const val LOG_TAG = "MainActivity"

/**
 * This is our launcher Activity
 *
 * Some general notes:
 * * Host Microphone Access must be enabled for Speech-to-Text to work on an emulator
 *
 * TODO:
 *  * Try to get faster STT response time: Update EndpointConfig in SherpaClient to have a short
 *  (~700ms) [com.k2fsa.sherpa.onnx.EndpointRule.minTrailingSilence] when we have a short
 *  (~2000 ms) [com.k2fsa.sherpa.onnx.EndpointRule.minUtteranceLength]
 *  * Try to get faster STT response time: enable hardware acceleration in SherpaClient ("nnapi")
 *
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    /**
     * We launch this when we want to show the user the system dialog that requests the record
     * permission.
     */
    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(LOG_TAG, "registerForActivityResult() -- granted: $granted")
            if (granted) viewModel.sendAction(micClickAction)
            else viewModel.sendPermissionAction(permissionDenied)
        }

    private val micClickAction: MicClickAction
        get() = MicClickAction(
            shouldShowRequestPermissionRationale(RECORD_AUDIO),
            isMicPermissionGranted()
        )

    private val permissionDenied
        get() = PermissionDenied(!shouldShowRequestPermissionRationale(RECORD_AUDIO))

    /**
     * Set up the UI and collect UI state from the ViewModel. Also, collect and launch [Intent]s
     * to start actions specified by user voice commands.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            viewModel.intentFlow.collect { intent ->
                startActivity(intent)
            }
        }
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val dialogState = viewModel.dialogState
            val transcript by viewModel.transcript.collectAsState("")
            MainUi(
                uiState = uiState,
                dialogState = dialogState,
                transcript = transcript,
                clickHandler = { viewModel.sendAction(micClickAction) },
                confirmHandler = {
                    viewModel.sendPermissionAction(DismissDialog)
                    requestPermissionLauncher.launch(RECORD_AUDIO)
                },
                dismissHandler = { viewModel.sendPermissionAction(DismissDialog) },
                permissionsRequiredHandler = { launchSettingsActivity() }
            )
        }
    }

    /**
     * Launch to microphone settings page
     */
    private fun launchSettingsActivity() {
        Log.i(LOG_TAG, "launchSettingsActivity()")
        viewModel.sendPermissionAction(PermissionAction.SettingsLaunched)
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }


    /**
     * Stop recording when the app is backgrounded
     */
    override fun onPause() {
        viewModel.stopRecording()
        super.onPause()
    }

    /**
     * We handle the case when user is returning from the app settings activity here.  The ViewModel
     * will initiate a mic click action on behalf of the user, if appropriate.
     */
    override fun onResume() {
        Log.i(LOG_TAG, "onResume() -- isMicPermissionGranted(): ${isMicPermissionGranted()}")
        super.onResume()
        viewModel.handleResume(micClickAction)
    }

    /**
     * Convenience method to check if we have mic permissions.
     */
    private fun isMicPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, RECORD_AUDIO) == PERMISSION_GRANTED


}

