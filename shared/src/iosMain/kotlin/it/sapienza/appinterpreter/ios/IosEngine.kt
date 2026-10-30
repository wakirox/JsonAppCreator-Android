package it.sapienza.appinterpreter.ios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.ktor.client.engine.darwin.Darwin
import it.sapienza.appinterpreter.alerts.AlertsViewModel
import it.sapienza.appinterpreter.calendar.CreateCalendarEventUseCase
import it.sapienza.appinterpreter.runtime.AppRepository
import it.sapienza.appinterpreter.runtime.CurrentConfiguration
import it.sapienza.appinterpreter.runtime.EnglishRuntimeStrings
import it.sapienza.appinterpreter.runtime.ScreenViewModel
import it.sapienza.appinterpreter.runtime.usecase.DeleteItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.FetchJsonUseCase
import it.sapienza.appinterpreter.runtime.usecase.FindMissingApiKeysUseCase
import it.sapienza.appinterpreter.runtime.usecase.InitialFormValuesUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadListItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadScreenUseCase
import it.sapienza.appinterpreter.runtime.usecase.LoadStoredItemsUseCase
import it.sapienza.appinterpreter.runtime.usecase.OpenLinkUseCase
import it.sapienza.appinterpreter.runtime.usecase.SaveItemUseCase
import it.sapienza.appinterpreter.runtime.usecase.SubmitFormUseCase
import it.sapienza.appinterpreter.runtime.usecase.UpdateItemUseCase
import it.sapienza.appinterpreter.security.ConfigurationKeys
import it.sapienza.appinterpreter.security.SecretStore
import it.sapienza.appinterpreter.storage.AppData
import it.sapienza.appinterpreter.storage.PhotoReader
import it.sapienza.appinterpreter.utils.httpclient.JsonHttpClient
import it.sapienza.appinterpreter.validation.ValidatedConfigurationParser
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.cancel
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * What Android's Koin module builds, for the iPhone app: the shared engine's use cases on iOS implementations, running
 * the JSON app [json] ([versionName] and [platform] name the app in its requests' User-Agent, [userAgent]).
 */
class IosEngine(
    private val json: String,
    versionName: String,
    platform: String,
    /** Where the user's API keys are kept (the Keychain), under [configurationId]: the app's identity for them. */
    secrets: SecretStore,
    private val configurationId: String,
    /** The build's keys for the bundled examples: Base64 of {"v3_the_movie_db": {"TMDB_API_KEY": "…"}}, or empty. */
    bundledKeys: String = "",
    /** The JPEG bytes of a kept photo by its "appkin-photo:" reference (null: none), for requests that send photos. */
    photoData: (String) -> NSData?
) {
    val userAgent: String = JsonHttpClient.userAgent(versionName, platform)

    private val photos = PhotoReader { reference -> photoData(reference)?.toByteArray() }
    private val strings = EnglishRuntimeStrings
    private val current = CurrentConfiguration { json }
    private val repository = AppRepository(current, ValidatedConfigurationParser(SharedValidator.validator))
    private val keys =
        ConfigurationKeys(secrets, ConfigurationKeys.decodeBundledValues(bundledKeys)) { configurationId }
    private val httpClient = JsonHttpClient(
        Darwin.create(),
        userAgent,
        keys::replaceForCurrentConfiguration,
        keys::replaceInHeaderForCurrentConfiguration
    )

    // One for the app, so every screen hears of the changes the others save.
    private val appData = AppData(UserDefaultsAppStore())
    private val loadStored = LoadStoredItemsUseCase(appData, repository)

    /**
     * A screen of the app: the view named [viewId] (none: the app's main view) shown with [payload], a JSON object as
     * text, and [title] instead of the app's name. These are the arguments Android's screens start with. [alerts] is
     * shared by the screens of one stack.
     */
    fun screen(viewId: String?, payload: String?, title: String?, alerts: AlertsViewModel): ScreenViewModel {
        val arguments = buildMap<String, Any?> {
            viewId?.let { put(ScreenViewModel.EXTRA_VIEW_ID, it) }
            payload?.let { put(ScreenViewModel.EXTRA_SCREEN_PAYLOAD, it) }
            title?.let { put(ScreenViewModel.EXTRA_SCREEN_TITLE, it) }
        }
        return ScreenViewModel(
            saved = SavedStateHandle(arguments),
            alerts = alerts,
            fetchJson = FetchJsonUseCase(httpClient, photos),
            loadListItems = LoadListItemsUseCase(FetchJsonUseCase(httpClient, photos), loadStored),
            initialFormValues = InitialFormValuesUseCase(),
            submitForm = SubmitFormUseCase(),
            createCalendarEvent = CreateCalendarEventUseCase(),
            saveItem = SaveItemUseCase(appData, repository),
            updateItem = UpdateItemUseCase(appData, repository),
            deleteItems = DeleteItemsUseCase(appData, repository),
            openLink = OpenLinkUseCase(),
            findMissingApiKeys = FindMissingApiKeysUseCase(current, keys),
            loadScreen = LoadScreenUseCase(repository, strings),
            strings = strings
        )
    }

    /** Ends [screen]'s requests: call it when the screen is gone. */
    fun dispose(screen: ScreenViewModel) = screen.viewModelScope.cancel()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).also { bytes ->
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}
