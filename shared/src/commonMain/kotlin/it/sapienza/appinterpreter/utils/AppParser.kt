package it.sapienza.appinterpreter.utils

import it.sapienza.appinterpreter.json.JsonSyntaxException
import it.sapienza.appinterpreter.json.JsonText
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model.ModelJson
import it.sapienza.appinterpreter.model.event.AlertMessage
import it.sapienza.appinterpreter.model.event.Event
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.event.ShowView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.json.JsonElement

class AppParser(val obj: String) {

    fun parseApplication2(): ModelApplication {
        /*
         * per facilitare la scrittura del modello si è deciso di dare la possibilità di riutilizzare determinati elementi
         * all'iterno del modello specificando solamente l'id di un elemento è possibile referenziarlo dalla lista generale dell'app
         * Per evitare problemi con la scrittura non è
         * possibile referenziare un elemento che si trova definito all'interno di un altro elemento*/

        val tree = try {
            JsonText.parse(obj)
        } catch (error: JsonSyntaxException) {
            throw ConfigurationValidationException("$", "invalid JSON: ${error.message}")
        }
        return parse(tree)
    }

    /** The app of an already read configuration [tree]. */
    fun parse(tree: JsonElement?): ModelApplication {
        ReferenceValidator().validate(tree)

        val model = try {
            ModelJson.decodeFromJsonElement(ModelApplication.serializer(), checkNotNull(tree))
        } catch (error: ConfigurationValidationException) {
            throw error
        } catch (error: Exception) {
            throw ConfigurationValidationException("$", "configuration cannot be parsed: ${error.message}")
        }

        val declaredActions = model.actions.toList()
        val declaredViews = model.views.toList()

        declaredActions.forEach { it.event?.let { event -> findView(event, model) } }
        declaredViews.forEach { findGenericView(it, model, addSelf = false) }
        if (!model.mainView.isReference()) findGenericView(model.mainView, model)

        checkModelConsistency(model)

        return model
    }

    private fun findView(ev: Event, model: ModelApplication) {
        when (ev) {
            is ShowView -> if (!ev.view.isReference()) {
                findGenericView(ev.view, model)
            }

            is RESTService -> {
                ev.thenDo?.let { findView(it, model) }
            }

            is AlertMessage -> {
                ev.thenDoOK?.let { findView(it, model) }
                ev.thenDoKO?.let { findView(it, model) }
            }
        }
    }

    private fun findGenericView(view: MView, model: ModelApplication, addSelf: Boolean = true) {
        when (view) {
            is ListView -> {
                findGenericView(view.itemView, model)
            }

            is Layout -> {
                view.views.forEach {
                    findGenericView(it, model)
                }
            }
        }

        view.action?.event?.let {
            findView(it, model)
        }
        view.action?.takeUnless { it.isReference() }?.let { action ->
            if (model.actions.none { it.id == action.id }) model.actions.add(action)
        }

        if (addSelf && !view.isReference()) {
            model.views.add(view)
        }
    }

    private fun checkModelConsistency(model: ModelApplication) {
        model.actions.firstOrNull { it.isReference() }?.let {
            throw ConfigurationValidationException("$.actions", "references cannot be registered as action definitions")
        }
        model.views.firstOrNull { it.isReference() }?.let {
            throw ConfigurationValidationException("$.views", "references cannot be registered as view definitions")
        }
    }
}
