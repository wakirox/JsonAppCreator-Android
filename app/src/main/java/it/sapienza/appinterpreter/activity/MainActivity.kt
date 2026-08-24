package it.sapienza.appinterpreter.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import it.sapienza.appinterpreter.ui.RendererScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

class MainActivity : AppCompatActivity() {

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
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme)

//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val model = initializeModel() ?: return
        setContent { AppTheme { RendererScreen(model, loading) } }

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

        supportActionBar?.title = application.app()?.name
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        application.app()?.let {
            it.author?.let {
                menu?.add(Menu.NONE, 1, Menu.NONE, "Author $it")?.let {
                    it.setIcon(R.drawable.baseline_perm_identity_black_24)
                }
            }

            menu?.add(Menu.NONE, 1, Menu.NONE, "Version ${it.version}")?.let {
                it.setIcon(R.drawable.baseline_merge_type_black_24)
            }
//            it.previousVersion?.let {
//                menu?.add(Menu.NONE, 2, Menu.NONE, "P.Version $it")?.let {
//                    it.setIcon(R.drawable.baseline_merge_type_black_24)
//                }
//            }

            it.changelog?.let {
                menu?.add(Menu.NONE, 3, Menu.NONE, "Changelog")?.let {
                    it.setIcon(R.drawable.baseline_track_changes_black_24)
                }
            }
        }


        val menuItem = menu?.addSubMenu("JSON configuration")?.let {
            it.setIcon(R.drawable.baseline_account_tree_black_24)
            it
        }
        menuItem?.add(Menu.NONE, 4, Menu.NONE, "Load configuration from device")?.let {
            it.setIcon(R.drawable.baseline_insert_drive_file_black_24)
        }
        menuItem?.add(Menu.NONE, 5, Menu.NONE, "View current configuration")?.let {
            it.setIcon(R.drawable.baseline_view_list_black_24)
        }

        val addSubMenu = menuItem?.addSubMenu("Sample configurations")
        addSubMenu?.add(Menu.NONE, 6, Menu.NONE, "Plain app")
        addSubMenu?.add(Menu.NONE, 8, Menu.NONE, "Form example")
        addSubMenu?.add(Menu.NONE, 7, Menu.NONE, "THE MOVIE DB")




        return super.onCreateOptionsMenu(menu)

    }

    fun showLoading() {
        runOnUiThread { loading = true }
    }

    fun hideLoading() {
        runOnUiThread { loading = false }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 3) {
            spawnInstance(
                context = this,
                changelog = application.app()!!.changelog!!
            )
        } else if (item.itemId == 4) {
            openJsonFile.launch("application/json")
        } else if (item.itemId == 5) {
            startActivity(Intent(this, JsonActivity::class.java))
        } else if (item.itemId == 6) {
            //simple
            DomainController.setJsonFile(R.raw.v3_app_sample, this)
            spawnNew(this)

        } else if (item.itemId == 7) {
            //the movie db
            DomainController.setJsonFile(R.raw.v3_the_movie_db, this)
            spawnNew(this)

        }else if (item.itemId == 8) {
            //the movie db
            DomainController.setJsonFile(R.raw.v3_app_form, this)
            spawnNew(this)

        }
        return super.onOptionsItemSelected(item)
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
