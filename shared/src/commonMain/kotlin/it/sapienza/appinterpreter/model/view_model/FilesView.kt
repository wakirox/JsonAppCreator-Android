package it.sapienza.appinterpreter.model.view_model

import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.helper.MView
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** The files at [mapping] (a files field's list): one chip each, which opens the file in an app that reads it. */
@Serializable
class FilesView(
    override val mapping: String,
    override val id: String? = null,
    override val action: Action? = null,
    override val data: JsonObject? = null,
    override val visibleIf: String? = null
) : MView()
