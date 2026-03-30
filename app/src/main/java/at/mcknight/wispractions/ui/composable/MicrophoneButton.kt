package at.mcknight.wispractions.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.mcknight.wispractions.R
import at.mcknight.wispractions.ui.theme.WisprActionsTheme

@Composable
fun MicrophoneButton(
    name: String, clickHandler: () -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            modifier = Modifier
                .background(Color.LightGray, CircleShape),
            onClick = { clickHandler() },
        ) {
            Icon(painter = painterResource(R.drawable.ic_mic), contentDescription = "Mic")
        }
        Text(name)
    }
}

@Preview(showBackground = true)
@Composable
fun MicrophoneButtonPreview() {
    WisprActionsTheme {
        MicrophoneButton("Hold to Talk")
    }
}
