package it.sapienza.appinterpreter.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import java.io.File

/** Builds the AI context from what the app bundles, and hands it to an AI app by clipboard or share sheet. */
object AskAi {
    private const val GUIDE_ASSET = "LLM_GUIDE.md"
    private const val FILE_NAME = "JSON-App-Creator-AI-context.md"

    fun context(context: Context, currentApp: String?, request: String?): String {
        fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }
        fun raw(id: Int) = context.resources.openRawResource(id).bufferedReader().use { it.readText() }
        return AiContext.build(
            guide = asset(GUIDE_ASSET),
            schemaJson = asset(ConfigurationValidator.ASSET_PATH),
            examples = AiContext.examples(raw(R.raw.v3_masterpieces), raw(R.raw.v3_race_signup), raw(R.raw.v3_pokedex)),
            currentApp = currentApp,
            request = request
        )
    }

    fun copy(context: Context, text: String, done: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("JSON App Creator", text))
        Toast.makeText(context, done, Toast.LENGTH_LONG).show()
    }

    /** Shares the context as a Markdown file, with a short message, to the app the user picks. */
    fun share(context: Context, markdown: String, request: String?) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, FILE_NAME).apply { writeText(markdown) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val message = "Create a JSON App Creator configuration following the attached file" +
            (request?.trim()?.takeIf { it.isNotEmpty() }?.let { ": $it" } ?: ".") +
            " Return only the JSON."
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, message)
            clipData = ClipData.newRawUri(FILE_NAME, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Ask an AI"))
    }

    /** The JSON in the clipboard's text, if it holds an AI answer with one. */
    fun jsonFromClipboard(context: Context): String? {
        val clip = context.getSystemService(ClipboardManager::class.java).primaryClip ?: return null
        val text = (0 until clip.itemCount).joinToString("\n") { clip.getItemAt(it).coerceToText(context).toString() }
        return AiContext.extractJson(text)
    }
}

/**
 * Asks for what to build, then copies the full prompt or shares it as a file. [currentApp], when given, can be
 * included so the AI changes it instead of starting over.
 */
@Composable
fun AskAiDialog(currentApp: String?, changeCurrentByDefault: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var request by rememberSaveable { mutableStateOf("") }
    var includeCurrent by rememberSaveable { mutableStateOf(changeCurrentByDefault && currentApp != null) }
    fun markdown() = AskAi.context(context, currentApp.takeIf { includeCurrent }, request)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ask an AI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The AI gets the app's complete format reference and examples. Paste its answer in the editor " +
                        "with Paste AI answer.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = request,
                    onValueChange = { request = it },
                    label = { Text(if (includeCurrent) "What to change" else "Describe the app") },
                    placeholder = { Text("A list of my favourite books, with a detail screen") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (currentApp != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeCurrent, onCheckedChange = { includeCurrent = it })
                        Text("Change the current app")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AskAi.share(context, markdown(), request)
                onDismiss()
            }) { Text("Share…") }
        },
        dismissButton = {
            TextButton(onClick = {
                AskAi.copy(context, markdown(), "Prompt copied: paste it into your AI app")
                onDismiss()
            }) { Text("Copy prompt") }
        }
    )
}
