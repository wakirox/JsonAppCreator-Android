package it.sapienza.appinterpreter

import android.app.Application
import it.sapienza.androidratio.appratio.BuildConfig
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.security.ConfigurationApiKeys

class AppRatioApplication : Application() {

    var app : ModelApplication? = null

    override fun onCreate() {
        super.onCreate()
        ConfigurationApiKeys.initialize(this, BuildConfig.CONFIG_API_KEYS_BASE64)
    }


}
