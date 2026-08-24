package it.sapienza.appinterpreter.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fasterxml.jackson.core.type.TypeReference
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.alerts.AlertUtils
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.extensions.getJsonExtra
import it.sapienza.appinterpreter.extensions.putExtraJson
import it.sapienza.appinterpreter.extensions.setApp
import it.sapienza.appinterpreter.utils.AppParser
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.ui.MainScreen
import org.json.JSONObject


fun spawnInstance(context: Context, model : MView) {
    val intent = Intent(context, MainActivity::class.java)
//    intent.putExtra("model", model)

    model.data?.let {
        intent.putExtraJson(it)
    }

    intent.putExtra("idView", model.id)

    context.startActivity(intent)

}

fun spawnNew(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

class MainActivity : ComponentActivity() {

    private var loading by mutableStateOf(false)

    private val openJsonFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalArgumentException("Unable to read the selected file")
            AppParser(text).parseApplication2()
            DomainController.setJsonFile(text, this)
            spawnNew(this)
        } catch (e: Exception) {
            AlertUtils.showAlert(
                this,
                "An error occurred while reading the model\n${e.message ?: "Unrecognized error"}"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Compose)
        super.onCreate(savedInstanceState)

//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val model = initializeModel() ?: return
        val appModel = checkNotNull(application.app())
        setContent {
            AppTheme {
                MainScreen(
                    title = appModel.name,
                    author = appModel.author,
                    version = appModel.version,
                    hasChangelog = appModel.changelog != null,
                    model = model,
                    loading = loading,
                    onChangelog = {
                        appModel.changelog?.let { changelog ->
                            spawnInstance(context = this, changelog = changelog)
                        }
                    },
                    onLoadConfiguration = { openJsonFile.launch("application/json") },
                    onViewConfiguration = {
                        startActivity(Intent(this, JsonActivity::class.java))
                    },
                    onLoadPlainSample = {
                        loadSampleConfiguration(R.raw.v3_app_sample)
                    },
                    onLoadFormSample = {
                        loadSampleConfiguration(R.raw.v3_app_form)
                    },
                    onLoadMovieSample = {
                        loadSampleConfiguration(R.raw.v3_the_movie_db)
                    },
                    onLoadPokedexSample = {
                        loadSampleConfiguration(R.raw.v3_pokedex)
                    }
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTaskRoot) {
                    AlertUtils.showAlert(
                        this@MainActivity,
                        "Are you sure you want to close the app?",
                        Runnable { finish() },
                        Runnable { }
                    )
                } else {
                    finish()
                }
            }
        })

    }

    private fun initializeModel(): MView? {
        return if (intent.hasExtra("idView")) {

            val view : MView =
                application.app()!!.viewBy(intent.getStringExtra("idView")!!)!!

            val typeRef: TypeReference<MutableMap<Any?, Any?>?> =
                object : TypeReference<MutableMap<Any?, Any?>?>() {}

            intent.getJsonExtra(typeRef)?.let { it ->
                view.data = it
            }

            view

        } else {

            val schema =
                DomainController.jsonFile(this)
            val parser = AppParser(schema)

            try {
                application.setApp(parser.parseApplication2())
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                AlertUtils.showAlert(
                    this, """An error occurred while parsing the model
                |${e.message ?: "Unrecognized error"}""".trimMargin()
                )
                return null
            }


            application.app()!!.viewBy(application.app()!!.mainView)!!

//            application.app()!!.initService?.let {
//                EventManager.evaluateEvent(this,it,null) //todo serve passare dei dati per l'init?
//            }

        }
    }

    fun showLoading() {
        runOnUiThread { loading = true }
    }

    fun hideLoading() {
        runOnUiThread { loading = false }
    }

    private fun loadSampleConfiguration(resourceId: Int) {
        DomainController.setJsonFile(resourceId, this)
        spawnNew(this)
    }

    fun pushViewWithId(id: String, obj: JSONObject?) {
        val view = application.app()!!.viewBy(id)!!
        obj?.let {
            if (view.dataObj == null) { view.dataObj = obj }
        }
        spawnInstance(this, view)
    }

    fun pushScreen(view: MView, obj: JSONObject?) {
        val _view = application.app()!!.viewBy(view)!!
        obj?.let {
//            if (_view.dataObj == null) {
                _view.dataObj = obj
//            }
        }
        spawnInstance(this, _view)
    }

}
