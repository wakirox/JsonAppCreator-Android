package it.sapienza.appinterpreter.extensions

import android.app.Application
import it.sapienza.appinterpreter.model.ModelApplication

fun Application.app() : ModelApplication? {
    return (this as? it.sapienza.appinterpreter.AppRatioApplication)?.app
}

fun Application.setApp(model : ModelApplication){
    (this as? it.sapienza.appinterpreter.AppRatioApplication)?.app = model
}