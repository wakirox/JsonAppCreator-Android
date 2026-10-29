package it.sapienza.appinterpreter.model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** A whole Appkin app, as its v3 configuration defines it. */
@Serializable
class ModelApplication(
    val name: String,
    val schemaVersion: Int? = null,
    val author: String? = null,
    val debugMode: Boolean? = false,
    val version: String,
    val changelog: String? = null,
    /** One emoji for the app's icon; without it, the icon is the name's first letter. */
    val icon: String? = null,
    val mainView: MView,
    @SerialName("views") private val declaredViews: List<MView> = emptyList(),
    @SerialName("actions") private val declaredActions: List<Action> = emptyList()
) {
    /** The reusable views: those declared, then every definition found inside them (AppParser adds those). */
    @Transient
    val views: MutableList<MView> = declaredViews.toMutableList()

    /** The reusable actions: those declared, then every definition found in the views (AppParser adds those). */
    @Transient
    val actions: MutableList<Action> = declaredActions.toMutableList()

    fun viewBy(id: String): MView? = views.find { it.id == id }

    fun viewBy(view: MView): MView? = if (view.isReference()) views.find { it.id == view.referenceId } else view

    fun actionBy(action: Action): Action? = if (action.isReference()) {
        actions.find {
            it.id == action.referenceId
        }
    } else {
        action
    }
}
