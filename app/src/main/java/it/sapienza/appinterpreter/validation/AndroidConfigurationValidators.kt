package it.sapienza.appinterpreter.validation

import android.content.Context
import java.io.InputStream

/** The validator with the schema packaged in the app's assets. */
fun ConfigurationValidator.Companion.fromAssets(context: Context): ConfigurationValidator =
    context.assets.open(ConfigurationValidator.ASSET_PATH).use { fromSchema(it) }

fun ConfigurationValidator.Companion.fromSchema(schemaInput: InputStream): ConfigurationValidator =
    fromSchema(schemaInput.bufferedReader(Charsets.UTF_8).readText())
