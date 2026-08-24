package it.sapienza.appinterpreter.model.event

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.model.view_model.helper.ViewObject

class ShowView(
    @JsonProperty("view") viewObject : ViewObject,
    mapping : String?,
    data : MutableMap<Any?, Any?>?
) : Event(mapping, data) {
    @JsonIgnore var view = viewObject.convert()
}
