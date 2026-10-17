package it.sapienza.appinterpreter.di

import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.runtime.ScreenViewModel
import it.sapienza.appinterpreter.runtime.usecase.CreateCalendarEventUseCase
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.FindMissingApiKeysUseCase
import it.sapienza.appinterpreter.runtime.usecase.InitialFormValuesUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadScreenUseCase
import it.sapienza.appinterpreter.runtime.usecase.SubmitFormUseCase
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** How the app's classes are built: the use cases, and the ViewModels that use them. */
val appModule = module {
    factory { FetchJsonUseCase() }
    factory { LoadListItemsUseCase(get()) }
    factory { InitialFormValuesUseCase() }
    factory { SubmitFormUseCase() }
    factory { CreateCalendarEventUseCase() }
    factory { FindMissingApiKeysUseCase(androidContext()) }
    factory { LoadScreenUseCase(androidContext()) }

    // A screen shares its activity's alerts, so the activity passes its AlertsViewModel in.
    viewModel { (alerts: AlertsViewModel) ->
        ScreenViewModel(
            application = androidApplication(),
            saved = get(),
            alerts = alerts,
            fetchJson = get(),
            loadListItems = get(),
            initialFormValues = get(),
            submitForm = get(),
            createCalendarEvent = get(),
            findMissingApiKeys = get(),
            loadScreen = get(),
        )
    }
}
