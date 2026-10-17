package it.sapienza.appinterpreter.runtime.usecase

import android.content.Context
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.runtime.AppRepository
import it.sapienza.appinterpreter.runtime.ScreenRuntimeState
import org.json.JSONObject

/** The current app and the screen to show: the view named [viewId] (none: the main view) with its payload. */
class LoadScreenUseCase(private val context: Context) {

    sealed interface Result {
        class Ready(val app: ModelApplication, val screen: ScreenRuntimeState) : Result
        /** Nothing to show: [message] says why. */
        class Failed(val message: String) : Result
    }

    operator fun invoke(viewId: String?, payload: String?): Result {
        val app = try {
            AppRepository.current(context)
        } catch (e: Exception) {
            return failed(R.string.configuration_invalid, e.message ?: context.getString(R.string.error_unrecognized))
        }
        val definition = if (viewId == null) {
            app.viewBy(app.mainView) ?: app.mainView
        } else {
            app.viewBy(viewId) ?: return failed(R.string.view_missing, viewId)
        }
        val data = try {
            payload?.let(::JSONObject)
        } catch (e: Exception) {
            return failed(R.string.screen_data_unrestorable, e.message.orEmpty())
        }
        return Result.Ready(app, ScreenRuntimeState.create(definition, data, navigatedViewId = viewId))
    }

    private fun failed(message: Int, vararg args: Any) = Result.Failed(context.getString(message, *args))
}
