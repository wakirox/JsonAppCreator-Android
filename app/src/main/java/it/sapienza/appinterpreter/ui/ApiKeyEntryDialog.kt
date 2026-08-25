package it.sapienza.appinterpreter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ApiKeyEntryDialog(
    names: Set<String>,
    savedNames: Set<String>,
    onConfirm: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val values = remember(names) {
        mutableStateMapOf<String, String>().apply { names.forEach { put(it, "") } }
    }
    val canConfirm = names.all { it in savedNames || values[it].orEmpty().isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuration API keys") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter the keys required by this configuration. They are encrypted in " +
                        "private app storage and are not added to the JSON."
                )
                names.forEach { name ->
                    OutlinedTextField(
                        value = values[name].orEmpty(),
                        onValueChange = { values[name] = it },
                        label = { Text(name) },
                        supportingText = if (name in savedNames) {
                            { Text("Leave blank to keep the saved value") }
                        } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canConfirm,
                onClick = { onConfirm(values.filterValues { it.isNotEmpty() }) }
            ) { Text("Load configuration") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 4.dp)) {
                Text("Cancel")
            }
        }
    )
}
