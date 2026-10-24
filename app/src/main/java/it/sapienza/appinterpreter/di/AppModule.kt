package it.sapienza.appinterpreter.di

import it.sapienza.appinterpreter.ai.AskAiViewModel
import it.sapienza.appinterpreter.ai.BuildAiPromptUseCase
import it.sapienza.appinterpreter.ai.LoadAppToChangeUseCase
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.calendar.CreateCalendarEventUseCase
import it.sapienza.appinterpreter.runtime.AndroidRuntimeStrings
import it.sapienza.appinterpreter.runtime.AppRepository
import it.sapienza.appinterpreter.runtime.CurrentConfiguration
import it.sapienza.appinterpreter.runtime.RuntimeStrings
import it.sapienza.appinterpreter.runtime.ScreenViewModel
import it.sapienza.appinterpreter.runtime.usecase.OpenLinkUseCase
import it.sapienza.appinterpreter.runtime.usecase.DeleteItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.FindMissingApiKeysUseCase
import it.sapienza.appinterpreter.runtime.usecase.InitialFormValuesUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadScreenUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadStoredItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.SaveItemUseCase
import it.sapienza.appinterpreter.runtime.usecase.SubmitFormUseCase
import it.sapienza.appinterpreter.runtime.usecase.UpdateItemUseCase
import it.sapienza.appinterpreter.security.ConfigurationApiKeys
import it.sapienza.appinterpreter.storage.AppData
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.utils.SharedPreferencesStore
import it.sapienza.appinterpreter.utils.httpclient.HttpClient
import it.sapienza.appinterpreter.validation.ConfigurationValidator
import it.sapienza.appinterpreter.validation.ValidatedConfigurationParser
import it.sapienza.appinterpreter.validation.fromAssets
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** How the app's classes are built: the shared engine's use cases on Android's implementations, and the ViewModels. */
val appModule = module {
    single<RuntimeStrings> { AndroidRuntimeStrings(androidContext()) }
    // The parsed app is shared by every screen, and parsed again only when the current JSON changes.
    single { CurrentConfiguration { DomainController.jsonFile(androidContext()) } }
    single { ValidatedConfigurationParser(ConfigurationValidator.fromAssets(androidContext())) }
    single { AppRepository(get(), get()) }
    factory { FetchJsonUseCase(HttpClient.json) }
    // One for the app, so every screen hears of the changes the others save.
    single { AppData(SharedPreferencesStore(androidContext(), "AppData")) }
    factory { LoadStoredItemsUseCase(get(), get()) }
    factory { SaveItemUseCase(get(), get()) }
    factory { UpdateItemUseCase(get(), get()) }
    factory { DeleteItemsUseCase(get(), get()) }
    factory { LoadListItemsUseCase(get(), get()) }
    factory { InitialFormValuesUseCase() }
    factory { SubmitFormUseCase() }
    factory { CreateCalendarEventUseCase() }
    factory { OpenLinkUseCase() }
    factory { FindMissingApiKeysUseCase(get(), ConfigurationApiKeys.keys) }
    factory { LoadScreenUseCase(get(), get()) }

    factory { LoadAppToChangeUseCase(androidContext()) }
    factory { BuildAiPromptUseCase(androidContext()) }
    viewModel { AskAiViewModel(saved = get(), loadAppToChange = get(), buildPrompt = get()) }

    // A screen shares its activity's alerts, so the activity passes its AlertsViewModel in.
    viewModel { (alerts: AlertsViewModel) ->
        ScreenViewModel(
            saved = get(),
            alerts = alerts,
            fetchJson = get(),
            loadListItems = get(),
            initialFormValues = get(),
            submitForm = get(),
            createCalendarEvent = get(),
            saveItem = get(),
            updateItem = get(),
            deleteItems = get(),
            openLink = get(),
            findMissingApiKeys = get(),
            loadScreen = get(),
            strings = get(),
        )
    }
}
