package it.sapienza.appinterpreter.utils.httpclient

import io.ktor.client.engine.okhttp.OkHttp
import it.sapienza.androidratio.appratio.BuildConfig
import it.sapienza.appinterpreter.platformName
import it.sapienza.appinterpreter.security.ConfigurationApiKeys
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object HttpClient {
    private val USER_AGENT = JsonHttpClient.userAgent(BuildConfig.VERSION_NAME, platformName)

    /**
     * The one OkHttp client for the app's requests and its images (Coil uses it too). It names the app in every
     * request's User-Agent, as sites such as Wikimedia require: they refuse OkHttp's default one.
     */
    val client: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()

    private val quick: OkHttpClient by lazy {
        client.newBuilder().callTimeout(IMAGE_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS).build()
    }

    /**
     * The HTTP status an image address answers with, after redirects (a HEAD request, so nothing is downloaded); null
     * when it doesn't answer in time.
     */
    suspend fun imageStatus(url: String): Int? = withContext(Dispatchers.IO) {
        runCatching { quick.newCall(Request.Builder().url(url).head().build()).execute().use { it.code } }.getOrNull()
    }

    private const val IMAGE_CHECK_TIMEOUT_SECONDS = 8L

    /** The shared engine's client for REST events and lists, running on [client]. */
    val json: JsonHttpClient by lazy {
        JsonHttpClient(
            OkHttp.create { preconfigured = client },
            USER_AGENT,
            ConfigurationApiKeys::replaceForCurrentConfiguration,
            ConfigurationApiKeys::replaceInHeaderForCurrentConfiguration
        )
    }
}
