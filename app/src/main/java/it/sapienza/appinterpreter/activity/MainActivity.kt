package it.sapienza.appinterpreter.activity

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.ai.AskAi
import it.sapienza.appinterpreter.calendar.AndroidCalendarWriter
import it.sapienza.appinterpreter.utils.ExternalApps
import it.sapienza.appinterpreter.alerts.AlertHost
import it.sapienza.appinterpreter.alerts.AlertUtils
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.runtime.ScreenViewModel
import it.sapienza.appinterpreter.security.ConfigurationApiKeys
import it.sapienza.appinterpreter.ui.ApiKeyEntryDialog
import it.sapienza.appinterpreter.ui.LocalScreen
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.ui.MainScreen
import it.sapienza.appinterpreter.utils.AppLibrary
import it.sapienza.appinterpreter.utils.of
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.utils.HomeShortcuts
import it.sapienza.appinterpreter.utils.ShareApp
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlinx.serialization.json.JsonObject
import androidx.compose.runtime.collectAsState


fun spawnInstance(context: Context, model: MView, payload: JsonObject? = model.data, title: String? = null) {
    val viewId = requireNotNull(model.id) { "A navigated view must have an id" }
    context.startActivity(screenIntent(context, viewId, payload, title))
}

private fun screenIntent(context: Context, viewId: String, payload: JsonObject?, title: String? = null): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(ScreenViewModel.EXTRA_VIEW_ID, viewId)
        payload?.let { putExtra(ScreenViewModel.EXTRA_SCREEN_PAYLOAD, it.toString()) }
        title?.let { putExtra(ScreenViewModel.EXTRA_SCREEN_TITLE, it) }
    }

/** Opens the current app in a fresh task on top of the home page, so Back leads there. */
fun spawnNew(context: Context) {
    val home = Intent(context, HomeActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivities(arrayOf(home, Intent(context, MainActivity::class.java)))
}

/**
 * One screen of the current JSON app. Its state and events live in [ScreenViewModel]; the activity draws it and does
 * what only an activity can: open the next screen, the calendar app, a link in another app, or the share sheet.
 */
class MainActivity : ComponentActivity() {

    private val alerts: AlertsViewModel by viewModels()
    private val screen: ScreenViewModel by viewModel { parametersOf(alerts) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Compose)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState != null) alerts.activityRecreated()

        // Opened from a home-screen shortcut: that saved app becomes the current one, before the screen loads it.
        if (savedInstanceState == null) intent.getStringExtra(HomeShortcuts.EXTRA_APP_ID)?.let { id ->
            AppLibrary.of(this).get(id)?.let { DomainController.setJsonFile(it.json, this, it.configurationId) }
                ?: Toast.makeText(this, R.string.app_deleted, Toast.LENGTH_LONG).show()
        }
        val shown = screen.screen
        val app = screen.app
        if (shown == null || app == null) {
            // Nothing to show but the alert that says why.
            setContent { AppTheme { AlertHost(alerts) } }
            return
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { screen.effects.collect(::handle) }
        }
        setContent {
            AppTheme {
                val missingKeys by screen.missingKeys.collectAsState()
                val loading by screen.loading.collectAsState()
                if (missingKeys == null) {
                    CompositionLocalProvider(LocalScreen provides screen) {
                        MainScreen(
                            title = screen.title ?: app.name,
                            author = app.author,
                            version = app.version,
                            changelog = app.changelog,
                            model = shown.definition,
                            modelData = shown.data,
                            loading = loading,
                            onEditApp = { EditorActivity.edit(this) },
                            onNewApp = { EditorActivity.newApp(this) },
                            onAskAi = { AskAiActivity.currentApp(this) },
                            onPasteFromAi = {
                                val answer = AskAi.clipboardText(this)
                                if (answer == null) {
                                    Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_LONG).show()
                                } else EditorActivity.aiAnswer(this, answer)
                            },
                            onHome = { HomeActivity.open(this) },
                            onAddToHome = { addToHomeScreen() },
                            onShare = { ShareApp.share(this, app.name, DomainController.jsonFile(this)) },
                            // Opened from a home-screen shortcut, the first screen has nothing to go back to.
                            onBack = if (isTaskRoot) null else onBackPressedDispatcher::onBackPressed
                        )
                    }
                }
                missingKeys?.let { keys ->
                    ApiKeyEntryDialog(
                        names = keys.names,
                        savedNames = keys.savedNames,
                        onConfirm = { values ->
                            ConfigurationApiKeys.saveForCurrentConfiguration(keys.json, values)
                            spawnNew(this)
                        },
                        // Cancelled: the app opens without its keys (what needs them can't load) and stays saved, with
                        // the menu reachable to add them later or open another app.
                        onDismiss = screen::skipKeys
                    )
                }
                AlertHost(alerts)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTaskRoot) {
                    AlertUtils.showAlert(
                        this@MainActivity,
                        getString(R.string.confirm_close),
                        Runnable { finish() },
                        Runnable { }
                    )
                } else {
                    finish()
                }
            }
        })
    }

    private fun handle(effect: ScreenViewModel.Effect) = when (effect) {
        is ScreenViewModel.Effect.OpenScreen -> spawnInstance(this, effect.view, effect.payload, effect.title)
        is ScreenViewModel.Effect.AddToCalendar ->
            if (!AndroidCalendarWriter(this).add(effect.entry)) alerts.show(getString(R.string.no_calendar_app)) else Unit
        is ScreenViewModel.Effect.OpenUrl -> try {
            startActivity(ExternalApps.open(effect.url, effect.scheme))
        } catch (_: ActivityNotFoundException) {
            alerts.show(getString(R.string.no_app_for_link, effect.url))
        }
        is ScreenViewModel.Effect.Share -> startActivity(ExternalApps.share(this, effect.text, getString(R.string.share_title)))
    }

    private fun addToHomeScreen() {
        val app = AppLibrary.of(this).save(DomainController.jsonFile(this), DomainController.configurationId(this))
        val message = when {
            app == null -> R.string.shortcut_needs_name
            !HomeShortcuts(this).pin(app) -> R.string.shortcuts_unsupported
            else -> null
        }
        message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
    }
}
