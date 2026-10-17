package it.sapienza.appinterpreter

import android.app.Application
import it.sapienza.androidratio.appratio.BuildConfig
import it.sapienza.appinterpreter.security.ConfigurationApiKeys

class AppRatioApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ConfigurationApiKeys.initialize(this, BuildConfig.CONFIG_API_KEYS_BASE64)
    }


}
