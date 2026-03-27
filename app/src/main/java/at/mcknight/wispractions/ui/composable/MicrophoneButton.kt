package at.mcknight.wispractions.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.mcknight.wispractions.ui.theme.WisprActionsTheme

@Composable
fun MicrophoneButton(
    name: String,
    modifier: Modifier = Modifier,
    clickHandler: () -> Unit
) {
    Button(
        modifier = Modifier
            .border(1.dp, Color.Red)
            .background(Color.Green),
        onClick = { clickHandler() },
    ) {
        Text(
            text = "Hello $name!",
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MicrophoneButtonPreview() {
    WisprActionsTheme {
        MicrophoneButton("Android"){}
    }
}
