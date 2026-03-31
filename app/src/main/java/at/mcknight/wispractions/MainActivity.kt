package at.mcknight.wispractions

import android.Manifest.permission.RECORD_AUDIO
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock.ACTION_SET_TIMER
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import org.koin.androidx.viewmodel.ext.android.viewModel

const val LOG_TAG = "MicPermissions"

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
     * Set up the UI and collect UI state from the ViewModel
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            viewModel.intentFlow
                .map { it.toIntent() }
                .collect {
                    startActivity(it)
                }
        }
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val dialogState = viewModel.dialogState
//            val transcript by viewModel.intentFlow.collectAsState("Nothing transcribed yet...")
            MainUi(
                uiState = uiState,
                dialogState = dialogState,
                transcript = "Nothing transcribed yet...",
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
     * Deserialize the JSON string into an [Intent]
     */
    private fun String.toIntent(): Intent {
        val (action, extras) = Json.decodeFromString<IntentData>(this)
        return Intent(ACTION_SET_TIMER).apply {
            extras.forEach { (key, value): Map.Entry<String, JsonElement> ->
                when (value) {
                    is JsonObject -> { /* value is JsonObject, access via value["key"] */ }
                    is JsonArray -> { /* value is JsonArray, iterate value */ }
                    is JsonPrimitive -> {
                        when {
                            value is JsonNull -> null
                            value.isString              -> putExtra(key, value.content)
                            value.booleanOrNull != null -> putExtra(key, value.boolean)
                            value.intOrNull != null     -> putExtra(key, value.int)
                            value.doubleOrNull != null  -> putExtra(key, value.double)
                            else -> Log.w("toIntent()", "unexpected value type: ${value.content}")
                        }
                    }
                }

            }
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

