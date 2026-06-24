package com.aiear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aiear.R
import com.aiear.ui.theme.AiearTheme

/**
 * The single spike screen: a title, a hint, a live status line, and one button that toggles
 * Start/Stop of the mic-FGS. The button is the foreground start surface (ADR-002).
 */
@Composable
fun CaptureScreen(
    capturing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.capture_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.capture_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Text(
                text =
                    stringResource(
                        if (capturing) R.string.status_running else R.string.status_idle,
                    ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Button(
                onClick = onToggle,
                modifier = Modifier.width(220.dp).padding(top = 16.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            if (capturing) R.string.action_stop else R.string.action_start,
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CaptureScreenIdlePreview() {
    AiearTheme {
        CaptureScreen(capturing = false, onToggle = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun CaptureScreenRunningPreview() {
    AiearTheme {
        CaptureScreen(capturing = true, onToggle = {})
    }
}
