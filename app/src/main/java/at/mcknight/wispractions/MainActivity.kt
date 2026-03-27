package at.mcknight.wispractions

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import at.mcknight.wispractions.PermissionAction.DismissDialog
import at.mcknight.wispractions.PermissionAction.PermissionDenied
import at.mcknight.wispractions.PermissionAction.PermissionGranted
import at.mcknight.wispractions.ui.composable.MicrophoneButton
import at.mcknight.wispractions.ui.composable.MicPermissionDialogs
import at.mcknight.wispractions.ui.theme.WisprActionsTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Launch Activity
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModel()

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i("MicPermissions", "registerForActivityResult() -- granted: $granted")
            if (granted) viewModel.sendPermissionAction(PermissionGranted)
            else viewModel.sendPermissionAction(
                PermissionDenied(!shouldShowRequestPermissionRationale(RECORD_AUDIO))
            )
        }

    private val micClickAction: MicClickAction
        get() = MicClickAction(
            shouldShowRequestPermissionRationale(RECORD_AUDIO),
            ContextCompat.checkSelfPermission(this, RECORD_AUDIO) == PERMISSION_GRANTED
        )

    /**
     * Set up the UI and collect UI state from the ViewModel
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val dialogState = viewModel.dialogState
            WisprActionsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {}) { innerPadding ->
                    MicrophoneButton(
                        name = uiState.name,
                        modifier = Modifier.padding(innerPadding),
                        clickHandler = { viewModel.sendAction(micClickAction)}
                    )
                    MicPermissionDialogs(
                        dialogState = dialogState,
                        confirmHandler = {
                            Log.i("MicPermissions", "MicPermissionDialogs.confirmHandler")
                            viewModel.sendPermissionAction(DismissDialog)
                            requestPermissionLauncher.launch(RECORD_AUDIO)
                        },
                        dismissHandler = { viewModel.sendPermissionAction(DismissDialog) }
                    )
                }
            }
        }
    }
}
