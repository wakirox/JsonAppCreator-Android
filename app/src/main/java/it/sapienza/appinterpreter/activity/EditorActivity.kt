package it.sapienza.appinterpreter.activity

import it.sapienza.appinterpreter.validation.fromAssets
import it.sapienza.appinterpreter.validation.fromSchema
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import io.ktor.client.engine.okhttp.OkHttp
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.json.sameValue
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.ai.AiContext
import it.sapienza.appinterpreter.ai.AskAi
import it.sapienza.appinterpreter.ai.ShareLinks
import it.sapienza.appinterpreter.editor.ConfigurationChecker
import it.sapienza.appinterpreter.editor.EditorScreen
import it.sapienza.appinterpreter.editor.SchemaSuggestions
import it.sapienza.appinterpreter.editor.Snippets
import it.sapienza.appinterpreter.editor.prettyJson
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.of
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Edits the active configuration, or writes a new app from a template, and runs the result. */
/** Reads shared chat pages with its own OkHttp client: a browser User-Agent, unlike the app's requests. */
private val shareLinkClient by lazy { ShareLinks.client(OkHttp.create()) }

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
        enableEdgeToEdge()
        val newApp = intent.getBooleanExtra(EXTRA_NEW_APP, false)
        // Shared from an AI app (or any app): the JSON in what was shared, if there is one.
        val shared = incomingText() ?: if (intent.action == Intent.ACTION_SEND) sharedText() else null
        val fromFile = intent.getBooleanExtra(EXTRA_FROM_FILE, false)
        val fromAiApp = shared?.let { AiContext.extractJson(it) }
        // A link to a shared chat instead of the answer: the chat is read to find the answer.
        val chatLink = shared?.takeIf { fromAiApp == null }?.let(ShareLinks::find)
        val schema = assets.open(ConfigurationValidator.ASSET_PATH).bufferedReader().use { it.readText() }
        val checker = ConfigurationChecker(ConfigurationValidator.fromSchema(schema.byteInputStream()))
        val suggestions = SchemaSuggestions(schema)

        setContent {
            AppTheme {
                var fromChat by rememberSaveable { mutableStateOf<String?>(null) }
                var chatUnreadable by rememberSaveable { mutableStateOf(false) }
                if (chatLink != null && fromChat == null) {
                    if (chatUnreadable) {
                        ChatNotReadDialog(ShareLinks.service(chatLink), onDismiss = ::finish)
                    } else {
                        ReadingChat(chatLink, onBack = ::finish)
                        LaunchedEffect(chatLink) {
                            val found = runCatching { ShareLinks.configuration(shareLinkClient, chatLink, AskAi.promptExamples(this@EditorActivity)) }.getOrNull()
                            if (found != null) fromChat = found else chatUnreadable = true
                        }
                    }
                    return@AppTheme
                }
                if (shared != null && fromAiApp == null && fromChat == null) {
                    NothingToOpenDialog(shared, onDismiss = ::finish)
                    return@AppTheme
                }
                val received = fromChat ?: fromAiApp
                EditorScreen(
                    title = when {
                        received != null && fromFile -> "Opened file"
                        received != null -> "From your AI app"
                        newApp -> "New app"
                        else -> "Edit this app"
                    },
                    initialText = when {
                        received != null -> prettyJson(received)
                        newApp -> Snippets.newApp.replaceFirst("\"My app\"", "\"${AppLibrary.of(this).freeName("My app")}\"")
                        else -> prettyJson(DomainController.jsonFile(this))
                    },
                    unsavedSource = when {
                        received == null -> null
                        fromFile -> "Opened from a file"
                        chatLink != null -> "From your shared ${ShareLinks.service(chatLink)} chat"
                        else -> "Shared from your AI app"
                    },
                    check = { text -> withContext(Dispatchers.Default) { checker.check(text) } },
                    suggestions = suggestions,
                    onRun = { text ->
                        // Unchanged content keeps its identity, and so its saved or bundled API keys.
                        val unchanged = chatLink == null && shared == null && !newApp && sameJson(text, DomainController.jsonFile(this))
                        if (unchanged) DomainController.setJsonFile(text, this, DomainController.configurationId(this))
                        else DomainController.setJsonFile(text, this)
                        spawnNew(this)
                        // Opened from another app's share sheet, the editor would otherwise stay in that app's task.
                        finish()
                    },
                    onSaveAs = { text ->
                        pendingSave = text
                        saveAs.launch(AiContext.fileName(text))
                    },
                    onBack = ::finish
                )
            }
        }
    }

    /** Text handed over by [aiAnswer] or [openedFile], through a file because it can be large. */
    private fun incomingText(): String? = intent.getStringExtra(EXTRA_INCOMING_FILE)?.let { path ->
        runCatching { File(path).readText() }.getOrNull()
    }

    private fun sameJson(a: String, b: String): Boolean =
        runCatching { JsonText.sameValue(JsonText.parse(a), JsonText.parse(b)) }.getOrDefault(false)

    /** The text shared to the app: the message, or the content of a shared text or JSON file. */
    private fun sharedText(): String? {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { return it }
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return null
        return runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
    }

    companion object {
        private const val EXTRA_NEW_APP = "new_app"
        private const val EXTRA_INCOMING_FILE = "incoming_file"
        private const val EXTRA_FROM_FILE = "from_file"

        fun edit(context: Context) = context.startActivity(Intent(context, EditorActivity::class.java))

        fun newApp(context: Context) =
            context.startActivity(Intent(context, EditorActivity::class.java).putExtra(EXTRA_NEW_APP, true))

        /** Opens an AI app's answer (copied, then pasted) like a shared one: "From your AI app". */
        fun aiAnswer(context: Context, answer: String) =
            context.startActivity(Intent(context, EditorActivity::class.java).putExtra(EXTRA_INCOMING_FILE, incoming(context, answer)))

        /** Opens a file's configuration: one with problems, to fix or send to the AI app, or one to look at first. */
        fun openedFile(context: Context, json: String) = context.startActivity(
            Intent(context, EditorActivity::class.java)
                .putExtra(EXTRA_INCOMING_FILE, incoming(context, json))
                .putExtra(EXTRA_FROM_FILE, true)
        )

        /**
         * Large text can't travel in an Intent (about 1 MB at most), so it goes through a file in the app's cache,
         * and only the path is passed.
         */
        private fun incoming(context: Context, text: String): String =
            File(File(context.cacheDir, "incoming").apply { mkdirs() }, "text-${System.nanoTime()}.txt")
                .apply { writeText(text) }.absolutePath
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

/** While a shared chat is read: what is happening, and that it's only reading a public page. */
@Composable
private fun ReadingChat(link: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text("Reading your shared ${ShareLinks.service(link)} chat…", style = MaterialTheme.typography.titleMedium)
        Text(
            "Appkin opens the public page of the link, as a browser would, and takes the last app the AI " +
                "wrote in it. There is no AI in this app.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

/** When a shared chat can't be read, or has no app in it. */
@Composable
private fun ChatNotReadDialog(service: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No app found in the chat") },
        text = {
            Text(
                "Appkin couldn't read an app from that $service chat: the page may have changed, be " +
                    "private, or have no JSON app in the AI's answers. In $service, copy the AI's answer (or open " +
                    "the file it made), then use Paste answer from your AI app."
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

