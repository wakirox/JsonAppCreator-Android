package it.sapienza.appinterpreter.model.action

import com.fasterxml.jackson.annotation.JsonProperty
import it.sapienza.appinterpreter.model.event.EventObject
import it.sapienza.appinterpreter.model.event.RESTService

class SearchAction(
    val hint : String = "Search",
    val name : String = "Search",
    val event : RESTService
    )