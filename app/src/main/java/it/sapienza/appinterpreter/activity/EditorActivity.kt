package it.sapienza.appinterpreter.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.core.content.IntentCompat
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.ai.AiContext
import it.sapienza.appinterpreter.editor.ConfigurationChecker
import it.sapienza.appinterpreter.editor.EditorScreen
import it.sapienza.appinterpreter.editor.SchemaSuggestions
import it.sapienza.appinterpreter.editor.Snippets
import it.sapienza.appinterpreter.editor.prettyJson
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Edits the active configuration, or writes a new app from a template, and runs the result. */
class EditorActivity : AppCompatActivity() {

    private var pendingSave: String? = null
    private val saveAs = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pendingSave ?: return@registerForActivityResult
        pendingSave = null
        uri ?: return@registerForActivityResult
        runCatching { contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) } }
            .onSuccess { Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(this, "Couldn't save the file", Toast.LENGTH_SHORT).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Compose)
        super.onCreate(savedInstanceState)
        val newApp = intent.getBooleanExtra(EXTRA_NEW_APP, false)
        // Shared from an AI app (or any app): the JSON in what was shared, if there is one.
        val shared = if (intent.action == Intent.ACTION_SEND) sharedText() else null
        val fromAiApp = shared?.let { AiContext.extractJson(it) }
        val initialText = when {
            fromAiApp != null -> prettyJson(fromAiApp)
            newApp -> Snippets.newApp
            else -> prettyJson(DomainController.jsonFile(this))
        }
        val schema = assets.open(ConfigurationValidator.ASSET_PATH).bufferedReader().use { it.readText() }
        val checker = ConfigurationChecker(ConfigurationValidator.fromSchema(schema.byteInputStream()))
        val suggestions = SchemaSuggestions(schema)

        setContent {
            AppTheme {
                if (shared != null && fromAiApp == null) {
                    NothingToOpenDialog(shared, onDismiss = ::finish)
                    return@AppTheme
                }
                EditorScreen(
                    title = when {
                        fromAiApp != null -> "From your AI app"
                        newApp -> "New app"
                        else -> "Edit configuration"
                    },
                    initialText = initialText,
                    fromAiApp = fromAiApp != null,
                    check = { text -> withContext(Dispatchers.Default) { checker.check(text) } },
                    suggestions = suggestions,
                    onRun = { text ->
                        DomainController.setJsonFile(text, this)
                        spawnNew(this)
                    },
                    onSaveAs = { text ->
                        pendingSave = text
                        saveAs.launch(fileName(text))
                    },
                    onBack = ::finish
                )
            }
        }
    }

    /** The text shared to the app: the message, or the content of a shared text or JSON file. */
    private fun sharedText(): String? {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { return it }
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return null
        return runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
    }

    private fun fileName(json: String): String {
        val name = Regex(""""name"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1).orEmpty()
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "app" }
        return "$slug.json"
    }

    companion object {
        private const val EXTRA_NEW_APP = "new_app"

        fun edit(context: Context) = context.startActivity(Intent(context, EditorActivity::class.java))

        fun newApp(context: Context) =
            context.startActivity(Intent(context, EditorActivity::class.java).putExtra(EXTRA_NEW_APP, true))
    }
}

/** Shown when something without JSON is shared to the app, such as a link to the chat instead of the AI's answer. */
@Composable
private fun NothingToOpenDialog(shared: String, onDismiss: () -> Unit) {
    val link = shared.trim().let { it.startsWith("http") && !it.contains(' ') }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No app to open") },
        text = {
            Text(
                (if (link) "That's a link to the chat, not the AI's answer. " else "What you shared has no JSON app in it. ") +
                    "In your AI app, share or copy the answer that contains the JSON. If you copied it, open the " +
                    "editor and use ⋮ → Paste answer from your AI app."
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
