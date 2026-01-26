package dev.pranav.applock.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.pranav.applock.R

@Composable
fun AccessibilityServiceGuideDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.8f),
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        title = {
            Text(
                text = stringResource(R.string.accessibility_guide_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.accessibility_guide_dialog_text_1),
                    //textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                //Text(
                //    text = stringResource(R.string.accessibility_guide_dialog_text_2),
                //    textAlign = TextAlign.Start,
                //    modifier = Modifier.fillMaxWidth()
                //)
                //
                //Spacer(modifier = Modifier.height(8.dp))
                //
                //Text(
                //    text = stringResource(R.string.accessibility_guide_dialog_text_3),
                //    textAlign = TextAlign.Start,
                //    modifier = Modifier.fillMaxWidth()
                //)
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.accessibility_guide_open_settings_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
