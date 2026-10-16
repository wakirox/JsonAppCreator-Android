package it.sapienza.appinterpreter.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.ai.AskAi
import it.sapienza.appinterpreter.ai.AskAiDialog
import it.sapienza.appinterpreter.alerts.AlertUtils
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.extensions.setApp
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.runtime.ScreenRuntimeState
import it.sapienza.appinterpreter.security.ConfigurationApiKeys
import it.sapienza.appinterpreter.ui.ApiKeyEntryDialog
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.ui.MainScreen
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.utils.HomeShortcuts
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import it.sapienza.appinterpreter.validation.ValidatedConfigurationParser
import org.json.JSONObject


private const val EXTRA_VIEW_ID = "idView"
private const val EXTRA_SCREEN_PAYLOAD = "screenPayload"
private const val STATE_VIEW_ID = "screen.viewId"
private const val STATE_SCREEN_PAYLOAD = "screen.payload"

fun spawnInstance(context: Context, model: MView, payload: JSONObject? = model.dataObj) {
    val viewId = requireNotNull(model.id) { "A navigated view must have an id" }
    context.startActivity(screenIntent(context, viewId, payload))
}

private fun screenIntent(context: Context, viewId: String, payload: JSONObject?): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_VIEW_ID, viewId)
        payload?.let { putExtra(EXTRA_SCREEN_PAYLOAD, it.toString()) }
    }

fun spawnNew(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

class MainActivity : ComponentActivity() {

    private var loading by mutableStateOf(false)
    private var loadingRequests = 0
    private var screenState: ScreenRuntimeState? = null
    private var pendingConfiguration by mutableStateOf<String?>(null)
    private var pendingApiKeyNames by mutableStateOf<Set<String>>(emptySet())
    private var savedApiKeyNames by mutableStateOf<Set<String>>(emptySet())
    private var askingAi by mutableStateOf(false)
    private val configurationParser by lazy(LazyThreadSafetyMode.NONE) {
        ValidatedConfigurationParser(ConfigurationValidator.fromAssets(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Compose)
        super.onCreate(savedInstanceState)

//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Opened from a home-screen shortcut: that saved app becomes the current one.
        if (savedInstanceState == null) intent.getStringExtra(HomeShortcuts.EXTRA_APP_ID)?.let { id ->
            AppLibrary.get(this, id)?.let { DomainController.setJsonFile(it.json, this, it.configurationId) }
                ?: Toast.makeText(this, "That app was deleted", Toast.LENGTH_LONG).show()
        }
        val screen = initializeScreen(savedInstanceState) ?: return
        screenState = screen
        val appModel = checkNotNull(application.app())
        prepareActiveConfigurationApiKeys()
        setContent {
            AppTheme {
                if (pendingConfiguration == null) {
                    MainScreen(
                        title = appModel.name,
                        author = appModel.author,
                        version = appModel.version,
                        changelog = appModel.changelog,
                        model = screen.definition,
                        modelData = screen.data,
                        loading = loading,
                        onEditApp = { EditorActivity.edit(this) },
                        onNewApp = { EditorActivity.newApp(this) },
                        onAskAi = { askingAi = true },
                        onPasteFromAi = {
                            val answer = AskAi.clipboardText(this)
                            if (answer == null) {
                                Toast.makeText(this, "The clipboard is empty: copy the answer in your AI app first", Toast.LENGTH_LONG).show()
                            } else EditorActivity.aiAnswer(this, answer)
                        },
                        onMyApps = { startActivity(Intent(this, MyAppsActivity::class.java)) },
                        onAddToHome = { addToHomeScreen() }
                    )
                }
                if (askingAi) {
                    AskAiDialog(
                        currentApp = DomainController.jsonFile(this),
                        changeCurrentByDefault = false,
                        onDismiss = { askingAi = false }
                    )
                }
                pendingConfiguration?.let { json ->
                    ApiKeyEntryDialog(
                        names = pendingApiKeyNames,
                        savedNames = savedApiKeyNames,
                        onConfirm = { values ->
                            ConfigurationApiKeys.saveForCurrentConfiguration(json, values)
                            spawnNew(this)
                        },
                        onDismiss = {
                            // Without its keys the current app can't run: fall back to the default example, so the
                            // menu stays reachable and another app can be opened.
                            clearPendingConfiguration()
                            loadSampleConfiguration(DomainController.DEFAULT_CONFIGURATION)
                        }
                    )
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTaskRoot) {
                    AlertUtils.showAlert(
                        this@MainActivity,
                        "Are you sure you want to close the app?",
                        Runnable { finish() },
                        Runnable { }
                    )
                } else {
                    finish()
                }
            }
        })

    }

    private fun initializeScreen(savedInstanceState: Bundle?): ScreenRuntimeState? {
        val viewId = savedInstanceState?.getString(STATE_VIEW_ID)
            ?: intent.getStringExtra(EXTRA_VIEW_ID)
        val appModel = if (viewId == null) {
            loadApplicationModel()
        } else {
            application.app() ?: loadApplicationModel()
        } ?: return null
        val definition = if (viewId == null) {
            appModel.viewBy(appModel.mainView) ?: appModel.mainView
        } else {
            appModel.viewBy(viewId) ?: run {
                AlertUtils.showAlert(this, "View '$viewId' does not exist in this configuration")
                return null
            }
        }
        val payloadText = savedInstanceState?.getString(STATE_SCREEN_PAYLOAD)
            ?: intent.getStringExtra(EXTRA_SCREEN_PAYLOAD)
        val payload = try {
            payloadText?.let(::JSONObject)
        } catch (e: Exception) {
            AlertUtils.showAlert(this, "Unable to restore screen data\n${e.message.orEmpty()}")
            return null
        }
        return ScreenRuntimeState.create(definition, payload, navigatedViewId = viewId)
    }

    private fun loadApplicationModel() = try {
        configurationParser.parse(DomainController.jsonFile(this)).also(application::setApp)
    } catch (e: Exception) {
        AlertUtils.showAlert(
            this,
            """The saved configuration is invalid
                |${e.message ?: "Unrecognized error"}""".trimMargin()
        )
        null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        screenState?.let { screen ->
            screen.navigatedViewId?.let { outState.putString(STATE_VIEW_ID, it) }
            screen.navigationPayload?.let {
                outState.putString(STATE_SCREEN_PAYLOAD, it.toString())
            }
        }
        super.onSaveInstanceState(outState)
    }

    fun showLoading() {
        runOnUiThread {
            loadingRequests += 1
            loading = true
        }
    }

    fun hideLoading() {
        runOnUiThread {
            loadingRequests = (loadingRequests - 1).coerceAtLeast(0)
            loading = loadingRequests > 0
        }
    }

    private fun loadSampleConfiguration(resourceId: Int) {
        DomainController.setJsonFile(resourceId, this)
        spawnNew(this)
    }

    private fun addToHomeScreen() {
        val app = AppLibrary.save(this, DomainController.jsonFile(this), DomainController.configurationId(this))
        val message = when {
            app == null -> "This app needs a name to be added to the home screen"
            !HomeShortcuts.pin(this, app) -> "Your home screen app can't add shortcuts"
            else -> null
        }
        message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
    }

    private fun prepareActiveConfigurationApiKeys() {
        val json = DomainController.jsonFile(this)
        val names = ConfigurationApiKeys.placeholderNames(json)
        val availableNames = ConfigurationApiKeys.availableNamesForCurrentConfiguration(json)
        if (names.all(availableNames::contains)) return

        pendingConfiguration = json
        pendingApiKeyNames = names
        savedApiKeyNames = availableNames
    }

    private fun clearPendingConfiguration() {
        pendingConfiguration = null
        pendingApiKeyNames = emptySet()
        savedApiKeyNames = emptySet()
    }

    fun pushViewWithId(id: String, obj: JSONObject?) {
        val view = application.app()?.viewBy(id)
        if (view == null) {
            AlertUtils.showAlert(this, "View '$id' does not exist in this configuration")
            return
        }
        spawnInstance(this, view, obj ?: view.dataObj)
    }

    fun pushScreen(view: MView, obj: JSONObject?) {
        val definition = application.app()?.viewBy(view)
        if (definition == null) {
            AlertUtils.showAlert(this, "View '${view.id.orEmpty()}' does not exist in this configuration")
            return
        }
        spawnInstance(this, definition, obj ?: definition.dataObj)
    }

}
