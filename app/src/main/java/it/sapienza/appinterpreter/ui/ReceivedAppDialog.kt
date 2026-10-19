package it.sapienza.appinterpreter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.utils.SharedApps

/** A valid app from a file, and what it does that matters before running it. */
data class ReceivedApp(
    val json: String,
    val name: String,
    val author: String?,
    val version: String,
    val webAddresses: List<String>,
    val apiKeys: Set<String>,
    val arrival: SharedApps.Arrival
)

/**
 * Shown when a file opens, since it may come from someone else: who made the app, the websites it connects to and
 * the API keys it needs. When a saved app already has its name, it replaces that one or is kept next to it.
 */
@Composable
fun ReceivedAppDialog(
    app: ReceivedApp,
    onOpen: () -> Unit,
    onKeepBoth: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sameName = app.arrival is SharedApps.Arrival.SameName
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    app.author?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.received_by, it, app.version) }
                        ?: stringResource(R.string.received_version, app.version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (app.webAddresses.isEmpty()) stringResource(R.string.received_offline)
                    else stringResource(R.string.received_web, app.webAddresses.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (app.apiKeys.isNotEmpty()) {
                    Text(stringResource(R.string.received_keys, app.apiKeys.joinToString(", ")), style = MaterialTheme.typography.bodyMedium)
                }
                if (sameName) {
                    Text(stringResource(R.string.received_same_name, app.name), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        // Stacked, since three or four actions don't fit side by side.
        confirmButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onOpen) {
                    Text(stringResource(if (sameName) R.string.received_replace else R.string.received_open))
                }
                if (sameName) OutlinedButton(onClick = onKeepBoth) { Text(stringResource(R.string.received_keep_both)) }
                TextButton(onClick = onEdit) { Text(stringResource(R.string.received_edit)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.received_cancel)) }
            }
        }
    )
}
