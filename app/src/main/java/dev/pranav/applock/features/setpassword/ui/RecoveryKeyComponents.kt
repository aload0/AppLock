package dev.pranav.applock.features.setpassword.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.pranav.applock.core.utils.RecoveryKeyManager
import dev.pranav.applock.core.utils.appLockRepository

@Composable
fun RecoveryKeyDialog(
    onDismiss: () -> Unit,
    onValidated: () -> Unit
) {
    val context = LocalContext.current
    val repo = context.appLockRepository()
    val recoveryKey = remember { mutableStateOf("") }
    val error = remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forgot passcode") },
        text = {
            Column {
                Text("Enter your recovery key to reset your current lock.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = recoveryKey.value,
                    onValueChange = {
                        recoveryKey.value = it
                        error.value = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Recovery key") },
                    supportingText = {
                        error.value?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (repo.validateRecoveryKey(recoveryKey.value.trim())) {
                    onValidated()
                    onDismiss()
                } else {
                    error.value = "Invalid recovery key"
                }
            }) { Text("Reset lock") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RecoveryKeyGeneratedDialog(
    recoveryKey: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Save your recovery key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This key is shown only once. Save it now to reset your lock later if you forget your passcode.")
                Text(
                    text = recoveryKey,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = RecoveryKeyManager.saveRecoveryKeyToDownloads(context, recoveryKey)
                result.onSuccess {
                    Toast.makeText(context, "Recovery key saved to $it", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, it.message ?: "Failed to save recovery key", Toast.LENGTH_LONG).show()
                }
            }) { Text("Save as TXT") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(recoveryKey))
                    Toast.makeText(context, "Recovery key copied", Toast.LENGTH_SHORT).show()
                }) { Text("Copy") }
                TextButton(onClick = onDismiss) { Text("Continue") }
            }
        }
    )
}
