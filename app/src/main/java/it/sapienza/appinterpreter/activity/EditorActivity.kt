package it.sapienza.appinterpreter.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import it.sapienza.androidratio.appratio.R
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
        val initialText = if (newApp) Snippets.newApp else prettyJson(DomainController.jsonFile(this))
        val schema = assets.open(ConfigurationValidator.ASSET_PATH).bufferedReader().use { it.readText() }
        val checker = ConfigurationChecker(ConfigurationValidator.fromSchema(schema.byteInputStream()))
        val suggestions = SchemaSuggestions(schema)

        setContent {
            AppTheme {
                EditorScreen(
                    title = if (newApp) "New app" else "Edit configuration",
                    initialText = initialText,
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
