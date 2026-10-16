package it.sapienza.appinterpreter.activity

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.unit.dp
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.utils.HomeShortcuts
import org.json.JSONObject

/** The saved apps (open one, add it to the home screen, or delete it), the examples, and opening a file. */
class MyAppsActivity : AppCompatActivity() {

    /** A JSON file goes through the same path as one opened from Files: API keys, problems, then the app. */
    private val openFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        startActivity(
            Intent(this, ManageFileInActivity::class.java).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Compose)
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                var apps by remember { mutableStateOf(AppLibrary.all(this)) }
                var deleting by remember { mutableStateOf<AppLibrary.SavedApp?>(null) }
                val current = runCatching { JSONObject(DomainController.jsonFile(this)).optString("name") }.getOrNull()

                Scaffold(topBar = {
                    TopAppBar(
                        title = { Text("My apps") },
                        navigationIcon = {
                            IconButton(onClick = ::finish) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = { TextButton(onClick = { openFile.launch("application/json") }) { Text("Open a file…") } }
                    )
                }) { padding ->
                    LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                        item { SectionHeader("Your apps") }
                        if (apps.isEmpty()) item {
                            Text(
                                "Apps you open or make appear here.",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(apps, key = { it.id }) { app ->
                            ListItem(
                                headlineContent = { Text(app.name) },
                                supportingContent = {
                                    Text(
                                        (if (app.name == current) "Open now · " else "") + "Saved " +
                                            DateUtils.getRelativeTimeSpanString(app.savedAt)
                                    )
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = {
                                            if (!HomeShortcuts.pin(this@MyAppsActivity, app)) {
                                                Toast.makeText(this@MyAppsActivity, "Your home screen app can't add shortcuts", Toast.LENGTH_LONG).show()
                                            }
                                        }) { Icon(Icons.Filled.Home, contentDescription = "Add to home screen") }
                                        IconButton(onClick = { deleting = app }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                        }
                                    }
                                },
                                modifier = Modifier.clickable {
                                    DomainController.setJsonFile(app.json, this@MyAppsActivity)
                                    spawnNew(this@MyAppsActivity)
                                }
                            )
                            HorizontalDivider()
                        }
                        item { SectionHeader("Examples") }
                        items(EXAMPLES) { (name, about, resource) ->
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = { Text(about) },
                                modifier = Modifier.clickable {
                                    DomainController.setJsonFile(resource, this@MyAppsActivity)
                                    spawnNew(this@MyAppsActivity)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                deleting?.let { app ->
                    AlertDialog(
                        onDismissRequest = { deleting = null },
                        title = { Text("Delete ${app.name}?") },
                        text = { Text("It's removed from My apps, and its home-screen shortcut stops working.") },
                        confirmButton = {
                            TextButton(onClick = {
                                AppLibrary.delete(this, app.id)
                                HomeShortcuts.disable(this, app.id)
                                apps = AppLibrary.all(this)
                                deleting = null
                            }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

/** The bundled examples: name, what they show, and the configuration. */
private val EXAMPLES = listOf(
    Triple("Masterpieces", "A gallery of famous paintings with a screen for each", R.raw.v3_masterpieces),
    Triple("Hello", "The smallest app: one screen of text", R.raw.v3_app_sample),
    Triple("Form", "Every kind of form field", R.raw.v3_app_form),
    Triple("Kanto Pokédex", "A list loaded from the web, with details", R.raw.v3_pokedex),
    Triple("The Movie DB", "Movies from an online API (needs your own API key)", R.raw.v3_the_movie_db)
)

