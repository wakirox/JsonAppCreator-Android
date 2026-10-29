package it.sapienza.appinterpreter.runtime.usecase

import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.runtime.AppRepository
import it.sapienza.appinterpreter.runtime.RuntimeStrings
import it.sapienza.appinterpreter.runtime.ScreenRuntimeState
import kotlinx.serialization.json.JsonObject

/** The current app and the screen to show: the view named [viewId] (none: the main view) with its payload. */
class LoadScreenUseCase(private val repository: AppRepository, private val strings: RuntimeStrings) {

    sealed interface Result {
        class Ready(val app: ModelApplication, val screen: ScreenRuntimeState) : Result

        /** Nothing to show: [message] says why. */
        class Failed(val message: String) : Result
    }

    operator fun invoke(viewId: String?, payload: String?): Result {
        val app = try {
            repository.current()
        } catch (e: Exception) {
            return Result.Failed(strings.configurationInvalid(e.message ?: strings.errorUnrecognized))
        }
        val definition = if (viewId == null) {
            app.viewBy(app.mainView) ?: app.mainView
        } else {
            app.viewBy(viewId) ?: return Result.Failed(strings.viewMissing(viewId))
        }
        val data = try {
            payload?.let {
                JsonText.parse(it) as? JsonObject
                    ?: throw IllegalArgumentException("The screen data is not a JSON object")
            }
        } catch (e: Exception) {
            return Result.Failed(strings.screenDataUnrestorable(e.message.orEmpty()))
        }
        return Result.Ready(app, ScreenRuntimeState.create(definition, data, navigatedViewId = viewId))
    }
}
