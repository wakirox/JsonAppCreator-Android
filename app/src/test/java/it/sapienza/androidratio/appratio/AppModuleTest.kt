package it.sapienza.androidratio.appratio

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.di.appModule
import it.sapienza.appinterpreter.security.ConfigurationKeys
import it.sapienza.appinterpreter.storage.AppStore
import it.sapienza.appinterpreter.utils.httpclient.JsonHttpClient
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Every class in the Koin module can be built: a missing binding fails here instead of crashing the app. */
class AppModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyDefinitionHasWhatItNeeds() {
        // Provided by Android or by the activity, not by the module; the API keys are set up by the Application, the
        // validator (from the schema asset), the HTTP client (on the app's OkHttp client) and the saved data's store (a
        // SharedPreferences file) are built in the module.
        appModule.verify(
            extraTypes = listOf(
                Context::class,
                Application::class,
                SavedStateHandle::class,
                AlertsViewModel::class,
                ConfigurationKeys::class,
                ConfigurationValidator::class,
                JsonHttpClient::class,
                AppStore::class
            )
        )
    }
}
