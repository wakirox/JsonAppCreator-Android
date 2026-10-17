package it.sapienza.androidratio.appratio

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.di.appModule
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Every class in the Koin module can be built: a missing binding fails here instead of crashing the app. */
class AppModuleTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyDefinitionHasWhatItNeeds() {
        // Provided by Android or by the activity, not by the module.
        appModule.verify(
            extraTypes = listOf(Context::class, Application::class, SavedStateHandle::class, AlertsViewModel::class)
        )
    }
}
