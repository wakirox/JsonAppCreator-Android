package it.sapienza.appinterpreter

import android.app.Application
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model.Layout

class AppRatioApplication() : Application() {

    var app : ModelApplication? = null

    override fun onCreate() {
        super.onCreate()

    }


}