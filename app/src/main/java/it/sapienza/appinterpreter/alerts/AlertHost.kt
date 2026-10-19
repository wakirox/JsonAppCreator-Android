package it.sapienza.appinterpreter.alerts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource

/** [viewModel]'s alert as a Material 3 dialog, which closes only through its buttons. */
@Composable
fun AlertHost(viewModel: AlertsViewModel) {
    val alert by viewModel.alertState.collectAsState()
    val shown = alert ?: return
    AlertDialog(
        onDismissRequest = {},
        text = { Text(shown.text) },
        confirmButton = {
            TextButton(onClick = { viewModel.answer(ok = true) }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = shown.onCancel?.let {
            { TextButton(onClick = { viewModel.answer(ok = false) }) { Text(stringResource(android.R.string.cancel)) } }
        }
    )
}
