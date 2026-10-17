package it.sapienza.appinterpreter

import android.app.Application
import it.sapienza.androidratio.appratio.BuildConfig
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import it.sapienza.appinterpreter.di.appModule
import it.sapienza.appinterpreter.utils.httpclient.HttpClient
import it.sapienza.appinterpreter.security.ConfigurationApiKeys
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class AppRatioApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        ConfigurationApiKeys.initialize(this, BuildConfig.CONFIG_API_KEYS_BASE64)
        // Robolectric creates an Application for every test in one process: start Koin afresh each time.
        stopKoin()
        startKoin {
            androidContext(this@AppRatioApplication)
            modules(appModule)
        }
    }

    /** Coil loads images through the app's OkHttp client, so they carry its User-Agent. */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { HttpClient.client })) }
        .build()


}
