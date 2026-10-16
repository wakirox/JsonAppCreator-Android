package it.sapienza.appinterpreter.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.validation.ConfigurationValidator

/**
 * Working with an AI app the user already has. JSON App Creator contains no AI: it shares instructions to the AI app
 * through Android's share sheet, and receives the AI's answer back the same way (see EditorActivity).
 */
object AskAi {
    private const val GUIDE_ASSET = "LLM_GUIDE.md"

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

    /** Opens the share sheet with [text], to send to the AI app the user picks. */
    fun shareToAiApp(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, "Share to your AI app")
        // Not to JSON App Creator itself, which receives AI answers through the same share sheet.
        chooser.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(ComponentName(context, "it.sapienza.appinterpreter.activity.EditorActivity"))
        )
        context.startActivity(chooser)
    }

    fun copy(context: Context, text: String, done: String) {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("JSON App Creator", text))
        Toast.makeText(context, done, Toast.LENGTH_LONG).show()
    }

    /** The clipboard's text, such as an answer copied from an AI app. */
    fun clipboardText(context: Context): String? {
        val clip = context.getSystemService(ClipboardManager::class.java).primaryClip ?: return null
        return (0 until clip.itemCount).joinToString("\n") { clip.getItemAt(it).coerceToText(context).toString() }
            .takeIf { it.isNotBlank() }
    }

    /** The JSON in the clipboard's text, if it holds an AI answer with one. */
    fun jsonFromClipboard(context: Context): String? = clipboardText(context)?.let(AiContext::extractJson)
}

/**
 * Starts working with an AI app: says plainly that there is no AI in JSON App Creator, explains the round trip, then
 * shares (or copies) the instructions and the request. [currentApp], when given, can be included so the AI changes
 * it instead of starting over.
 */
@Composable
fun AskAiDialog(currentApp: String?, changeCurrentByDefault: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var request by rememberSaveable { mutableStateOf("") }
    var includeCurrent by rememberSaveable { mutableStateOf(changeCurrentByDefault && currentApp != null) }
    fun prompt() = AskAi.context(context, currentApp.takeIf { includeCurrent }, request)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use your AI app") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "JSON App Creator has no AI inside. It hands instructions to an AI app you already use, " +
                        "such as ChatGPT, Claude or Gemini, and nothing is sent anywhere else.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "1. Describe your app and share it to your AI app.\n" +
                        "2. Chat there until you like the result.\n" +
                        "3. Share the AI's answer back to JSON App Creator: it opens in the editor, checked and " +
                        "ready to run.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = request,
                    onValueChange = { request = it },
                    label = { Text(if (includeCurrent) "What to change" else "Describe your app") },
                    placeholder = { Text("A list of my favourite books, with a screen for each") },
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
                AskAi.shareToAiApp(context, prompt())
                onDismiss()
            }) { Text("Share to AI app…") }
        },
        dismissButton = {
            TextButton(onClick = {
                AskAi.copy(context, prompt(), "Copied: paste it into your AI app")
                onDismiss()
            }) { Text("Copy") }
        }
    )
}
