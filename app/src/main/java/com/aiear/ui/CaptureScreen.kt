package com.aiear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * The single spike screen. S1: a Start/Stop button that is the foreground mic-FGS start surface
 * (ADR-002). S2 adds a "pair headphones" action + a paired-status line: once paired, connecting
 * the headphones arms capture from the background via CompanionCaptureService (no UI needed).
 */
@Composable
fun CaptureScreen(
    capturing: Boolean,
    paired: Boolean,
    onToggle: () -> Unit,
    onPair: () -> Unit,
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
                text = stringResource(R.string.s2_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Text(
                text =
                    stringResource(
                        if (paired) R.string.status_paired else R.string.status_unpaired,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            OutlinedButton(
                onClick = onPair,
                modifier = Modifier.width(220.dp).padding(top = 8.dp, bottom = 16.dp),
            ) {
                Text(text = stringResource(R.string.action_pair))
            }
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
        CaptureScreen(capturing = false, paired = false, onToggle = {}, onPair = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun CaptureScreenRunningPreview() {
    AiearTheme {
        CaptureScreen(capturing = true, paired = true, onToggle = {}, onPair = {})
    }
}
