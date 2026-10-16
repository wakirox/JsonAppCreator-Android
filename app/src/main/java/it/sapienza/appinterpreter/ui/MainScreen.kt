package it.sapienza.appinterpreter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
    hasChangelog: Boolean,
    model: MView,
    modelData: JSONObject?,
    loading: Boolean,
    onChangelog: () -> Unit,
    onLoadConfiguration: () -> Unit,
    onEditConfiguration: () -> Unit,
    onNewApp: () -> Unit,
    onAskAi: () -> Unit,
    onLoadMasterpiecesSample: () -> Unit,
    onLoadPlainSample: () -> Unit,
    onLoadFormSample: () -> Unit,
    onLoadMovieSample: () -> Unit,
    onLoadPokedexSample: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
                            contentDescription = "Open application menu"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        author?.let {
                            InformationMenuItem(
                                text = "Author $it",
                                iconResource = R.drawable.baseline_perm_identity_24
                            )
                        }
                        InformationMenuItem(
                            text = "Version $version",
                            iconResource = R.drawable.baseline_merge_type_24
                        )
                        if (hasChangelog) {
                            ActionMenuItem(
                                text = "Changelog",
                                iconResource = R.drawable.baseline_track_changes_24,
                                closeMenu = { menuExpanded = false },
                                onClick = onChangelog
                            )
                        }

                        HorizontalDivider()
                        MenuSectionLabel("JSON configuration")
                        ActionMenuItem(
                            text = "Load configuration from device",
                            iconResource = R.drawable.baseline_insert_drive_file_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onLoadConfiguration
                        )
                        ActionMenuItem(
                            text = "Edit configuration",
                            iconResource = R.drawable.baseline_view_list_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onEditConfiguration
                        )
                        ActionMenuItem(
                            text = "New app",
                            iconResource = R.drawable.baseline_insert_drive_file_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onNewApp
                        )
                        ActionMenuItem(
                            text = "Use your AI app…",
                            iconResource = R.drawable.baseline_track_changes_24,
                            closeMenu = { menuExpanded = false },
                            onClick = onAskAi
                        )

                        HorizontalDivider()
                        MenuSectionLabel("Sample configurations")
                        ActionMenuItem(
                            text = "Masterpieces",
                            closeMenu = { menuExpanded = false },
                            onClick = onLoadMasterpiecesSample
                        )
                        ActionMenuItem(
                            text = "Plain app",
                            closeMenu = { menuExpanded = false },
                            onClick = onLoadPlainSample
                        )
                        ActionMenuItem(
                            text = "Form example",
                            closeMenu = { menuExpanded = false },
                            onClick = onLoadFormSample
                        )
                        ActionMenuItem(
                            text = "THE MOVIE DB",
                            closeMenu = { menuExpanded = false },
                            onClick = onLoadMovieSample
                        )
                        ActionMenuItem(
                            text = "KANTO POKÉDEX",
                            closeMenu = { menuExpanded = false },
                            onClick = onLoadPokedexSample
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
}

@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun InformationMenuItem(text: String, iconResource: Int) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = {},
        enabled = false,
        leadingIcon = {
            Icon(painterResource(iconResource), contentDescription = null)
        }
    )
}

@Composable
private fun ActionMenuItem(
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
