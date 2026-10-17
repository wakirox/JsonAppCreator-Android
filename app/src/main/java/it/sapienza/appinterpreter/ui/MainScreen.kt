package it.sapienza.appinterpreter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.model.view_model.helper.MView
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    title: String,
    author: String?,
    version: String,
    changelog: String?,
    model: MView,
    modelData: JSONObject?,
    loading: Boolean,
    onEditApp: () -> Unit,
    onNewApp: () -> Unit,
    onAskAi: () -> Unit,
    onPasteFromAi: () -> Unit,
    onHome: () -> Unit,
    onAddToHome: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_more_vert_24),
                            contentDescription = stringResource(R.string.menu_open)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        ActionMenuItem(
                            text = stringResource(R.string.menu_edit_app),
                            iconResource = R.drawable.baseline_view_list_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onEditApp
                        )
                        ActionMenuItem(
                            text = stringResource(R.string.menu_new_app),
                            iconResource = R.drawable.baseline_insert_drive_file_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onNewApp
                        )
                        HorizontalDivider()
                        ActionMenuItem(
                            text = stringResource(R.string.menu_ask_ai),
                            iconResource = R.drawable.baseline_track_changes_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onAskAi
                        )
                        ActionMenuItem(
                            text = stringResource(R.string.menu_paste_ai),
                            iconResource = R.drawable.baseline_merge_type_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onPasteFromAi
                        )
                        HorizontalDivider()
                        ActionMenuItem(
                            text = stringResource(R.string.menu_home),
                            iconResource = R.drawable.baseline_account_tree_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onHome
                        )
                        ActionMenuItem(
                            text = stringResource(R.string.menu_add_to_home_screen),
                            iconResource = R.drawable.baseline_add_photo_alternate_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onAddToHome
                        )
                        ActionMenuItem(
                            text = stringResource(R.string.menu_about),
                            iconResource = R.drawable.baseline_perm_identity_24,
                            closeMenu = { menuExpanded = false },
                            onClick = { aboutOpen = true }
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            RendererScreen(model = model, data = modelData, loading = loading)
        }
    }

    if (aboutOpen) {
        AlertDialog(
            onDismissRequest = { aboutOpen = false },
            title = { Text(title) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.bodyMedium)
                    author?.let { Text(stringResource(R.string.about_author, it), style = MaterialTheme.typography.bodyMedium) }
                    changelog?.let {
                        Text(stringResource(R.string.about_whats_new), style = MaterialTheme.typography.titleSmall)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        stringResource(R.string.about_made_with),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { aboutOpen = false }) { Text(stringResource(R.string.about_ok)) } }
        )
    }
}

/** A ⋮ menu entry that closes the menu, then acts. The home page's menu uses it too. */
@Composable
fun ActionMenuItem(
    text: String,
    closeMenu: () -> Unit,
    onClick: () -> Unit,
    iconResource: Int? = null
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = {
            closeMenu()
            onClick()
        },
        leadingIcon = iconResource?.let { resourceId ->
            {
                Icon(painterResource(resourceId), contentDescription = null)
            }
        }
    )
}
