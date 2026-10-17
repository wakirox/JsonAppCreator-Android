package it.sapienza.appinterpreter.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.ai.AiContext
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.sapienza.appinterpreter.alerts.AlertHost
import it.sapienza.appinterpreter.alerts.AlertUtils
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.security.ConfigurationApiKeys
import it.sapienza.appinterpreter.ui.ApiKeyEntryDialog
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import it.sapienza.appinterpreter.validation.ValidatedConfigurationParser
import java.io.BufferedReader
import java.io.InputStreamReader

class ManageFileInActivity : AppCompatActivity() {
    private val alerts: AlertsViewModel by viewModels()
    /** A file that needs API keys, asked for before it opens. */
    private class NeedsKeys(val text: String, val names: Set<String>, val savedNames: Set<String>)
    private var needsKeys by mutableStateOf<NeedsKeys?>(null)
    private val configurationParser by lazy(LazyThreadSafetyMode.NONE) {
        ValidatedConfigurationParser(ConfigurationValidator.fromAssets(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Compose)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState != null) alerts.activityRecreated()
        setContent {
            AppTheme {
                needsKeys?.let { file ->
                    ApiKeyEntryDialog(
                        names = file.names,
                        savedNames = file.savedNames,
                        onConfirm = { values ->
                            ConfigurationApiKeys.save(file.text, values)
                            commitConfiguration(file.text)
                        },
                        onDismiss = { finish() }
                    )
                }
                AlertHost(alerts)
            }
        }
        handleIntent()
    }

    private fun handleIntent() {
        try {
            val text = intent?.data?.let(contentResolver::openInputStream)?.use {
                BufferedReader(InputStreamReader(it)).readText()
            } ?: throw IllegalArgumentException("Unable to read the selected file")

            try {
                configurationParser.parse(text)
            } catch (problems: Exception) {
                if (AiContext.extractJson(text) != null) {
                    EditorActivity.fileWithProblems(this, text)
                    finish()
                    return
                }
                throw problems
            }
            val names = ConfigurationApiKeys.placeholderNames(text)
            if (names.isEmpty()) {
                commitConfiguration(text)
                return
            }
            val savedNames = names.filterTo(mutableSetOf()) {
                ConfigurationApiKeys.hasSavedValue(text, it)
            }
            needsKeys = NeedsKeys(text, names, savedNames)

        } catch (e: Exception) {
            AlertUtils.showAlert(
                this, """Configuration could not be loaded
                    |
                    |${e.message ?: "Unrecognized error"}""".trimMargin(), Runnable {
                    finish()
                }
            )
        }
    }

    private fun commitConfiguration(text: String) {
        DomainController.setJsonFile(text, this)
        spawnNew(this)
        finish()
    }
}
