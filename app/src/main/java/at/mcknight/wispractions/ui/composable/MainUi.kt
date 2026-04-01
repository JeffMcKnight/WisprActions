package at.mcknight.wispractions.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    transcript: String,
    clickHandler: () -> Unit,
    confirmHandler: () -> Unit,
    dismissHandler: () -> Unit,
    permissionsRequiredHandler: () -> Unit,
) {
    WisprActionsTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(verticalArrangement = Arrangement.Top){
                // Title
                Text(
                    modifier = Modifier
                        .padding(64.dp)
                        .fillMaxWidth(),
                    text = "Start a Timer",
                    textAlign = TextAlign.Center,
                    fontSize = 32.sp
                )
                // Transcribed text
                Text(
                    modifier = Modifier
                        .padding(64.dp)
                        .fillMaxWidth(),
                    text = transcript,
                    textAlign = TextAlign.Left,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                MicrophoneButton(
                    name = uiState.name,
                    clickHandler = { clickHandler() }
                )
            }
            MicPermissionDialogs(
                dialogState = dialogState,
                confirmHandler = { confirmHandler() },
                dismissHandler = { dismissHandler() },
                permissionsRequiredHandler = { permissionsRequiredHandler() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainUiPreview() {
    WisprActionsTheme {
        MainUi(
            MainUiState(),
            dialogState = null,
            transcript = "",
            clickHandler = { },
            confirmHandler = { },
            dismissHandler = { },
            permissionsRequiredHandler = { },
        )
    }
}
