package it.sapienza.appinterpreter.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fasterxml.jackson.core.type.TypeReference
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.alerts.AlertUtils
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.extensions.getJsonExtra
import it.sapienza.appinterpreter.extensions.putExtraJson
import it.sapienza.appinterpreter.extensions.setApp
import it.sapienza.appinterpreter.model.screen.Screen
import it.sapienza.appinterpreter.utils.AppParser
import it.sapienza.appinterpreter.ContainerConfiguration
import it.sapienza.appinterpreter.utils.DomainController
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader


fun spawnInstance(context: Context, model: Screen) {
    val intent = Intent(context, MainActivity::class.java)
//    intent.putExtra("model", model)

    model.data?.let {
        intent.putExtraJson(it)
    }

    intent.putExtra("idScreen", model.id)

    context.startActivity(intent)

}

fun spawnNew(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme)

//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_main)

        if (intent.hasExtra("idScreen")) {

            val screen: Screen =
                application.app()!!.screenById(intent.getStringExtra("idScreen")!!)!!

            val typeRef: TypeReference<MutableMap<Any?, Any?>?> =
                object : TypeReference<MutableMap<Any?, Any?>?>() {}

            intent.getJsonExtra(typeRef)?.let { it ->
                screen.data = it
            }

            addContainer(screen)

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
                return
            }


            addContainer(application.app()!!.screenBy(application.app()!!.main)!!)

//            application.app()!!.initService?.let {
//                EventManager.evaluateEvent(this,it,null) //todo serve passare dei dati per l'init?
//            }

        }


        supportActionBar?.title = application.app()?.name

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
            it.previousVersion?.let {
                menu?.add(Menu.NONE, 2, Menu.NONE, "P.Version $it")?.let {
                    it.setIcon(R.drawable.baseline_merge_type_black_24)
                }
            }

            it.changelog?.let {
                menu?.add(Menu.NONE, 3, Menu.NONE, "Changelog")?.let {
                    it.setIcon(R.drawable.baseline_track_changes_black_24)
                }
            }
        }


        val menuItem = menu?.addSubMenu("JSON model")?.let {
            it.setIcon(R.drawable.baseline_account_tree_black_24)
            it
        }
        menuItem?.add(Menu.NONE, 4, Menu.NONE, "Load model from device")?.let {
            it.setIcon(R.drawable.baseline_insert_drive_file_black_24)
        }
        menuItem?.add(Menu.NONE, 5, Menu.NONE, "View current model")?.let {
            it.setIcon(R.drawable.baseline_view_list_black_24)
        }

        val addSubMenu = menuItem?.addSubMenu("Sample models")
        addSubMenu?.add(Menu.NONE, 6, Menu.NONE, "Plain app")
        addSubMenu?.add(Menu.NONE, 8, Menu.NONE, "Form example")
        addSubMenu?.add(Menu.NONE, 7, Menu.NONE, "THE MOVIE DB")




        return super.onCreateOptionsMenu(menu)

    }

    fun showLoading() {
//        runOnUiThread {
//            this.spin_kit.visibility = View.VISIBLE
//        }
    }

    fun hideLoading() {
//        runOnUiThread {
//            this.spin_kit.visibility = View.GONE
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 3) {
            spawnInstance(
                context = this,
                changelog = application.app()!!.changelog!!
            )
        } else if (item.itemId == 4) {
            val intent = Intent()
                .setType("application/json")
                .setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select a json model"), 111)
        } else if (item.itemId == 5) {
            startActivity(Intent(this, JsonActivity::class.java))
        } else if (item.itemId == 6) {
            //simple
            DomainController.setJsonFile(R.raw.app_sample, this)
            spawnNew(this)

        } else if (item.itemId == 7) {
            //the movie db
            DomainController.setJsonFile(R.raw.themoviedb, this)
            spawnNew(this)

        }else if (item.itemId == 8) {
            //the movie db
            DomainController.setJsonFile(R.raw.app_form, this)
            spawnNew(this)

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {

            try {
                data?.data?.let {
                    contentResolver.openInputStream(it)
                }?.let {
                    val r = BufferedReader(InputStreamReader(it))

                    val text = r.readText()
                    val parser = AppParser(text)

                    try {
                        //verifico se il file è buono altrimenti mostro l'errore e basta
                        parser.parseApplication2()
                        DomainController.setJsonFile(text, this)
                        spawnNew(this)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        AlertUtils.showAlert(
                            this, """An error occurred while parsing the model
                |${e.message ?: "Unrecognized error"}""".trimMargin()
                        )
                        return
                    }
                }

            } catch (e: Exception) { // If the app failed to attempt to retrieve the error file, throw an error alert
                AlertUtils.showAlert(
                    this,
                    "Sorry, but there was an error reading in the file"
                )
            }

        }
    }

    fun filePermissionCheck(runnable: Runnable) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_CONTACTS
                )
            ) {

                AlertUtils.showAlert(
                    this,
                    "If you want to load a model from your device you have to accept the permission"
                )
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    11
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            11 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    fun addContainer(model: Screen) {
        ContainerConfiguration.createContainer(
            this,
            application.app()!!,
            model,
            linearLayout
        )
    }

    fun clear() {
        linearLayout.removeAllViews()
    }

    fun pushScreenWithId(id: String, obj: JSONObject?) {
        val screen = application.app()!!.screenById(id)!!
        obj?.let {
            if (screen.inheritedData == true) {
                screen.dataObj = obj
            } else {
                if (screen.dataObj == null) {
                    screen.dataObj = obj
                }
            }
        }
        spawnInstance(this, screen)
    }

    fun pushScreen(screen: Screen, obj: JSONObject?) {
        val screen = application.app()!!.screenBy(screen)!!
        obj?.let {
            if (screen.inheritedData == true) {
                screen.dataObj = obj
            } else {
                if (screen.dataObj == null) {
                    screen.dataObj = obj
                }
            }
        }
        spawnInstance(this, screen)
    }

    override fun onBackPressed() {
        if(isTaskRoot){
            AlertUtils.showAlert(this,"Are you sure you want to close the app?", Runnable {
                super.onBackPressed()
            }, Runnable {  })
        }else{
            super.onBackPressed()
        }
    }


}
