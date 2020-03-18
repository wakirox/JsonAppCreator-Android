package it.sapienza.appinterpreter.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.sapienza.appinterpreter.alerts.AlertUtils
import it.sapienza.appinterpreter.utils.AppParser
import it.sapienza.appinterpreter.utils.DomainController
import java.io.BufferedReader
import java.io.InputStreamReader

class ManageFileInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent()
    }

    private fun handleIntent() {
        try {
            intent?.data?.let {
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
                    finish()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    AlertUtils.showAlert(
                        this, """An error occurred while parsing the model
                |${e.message ?: "Unrecognized error"}""".trimMargin(), Runnable{
                            this.finish()
                        }
                    )
                    return
                }
            }

        } catch (e: Exception) { // If the app failed to attempt to retrieve the error file, throw an error alert
            AlertUtils.showAlert(
                this,
                "Sorry, but there was an error reading in the file", Runnable{
                    this.finish()
                }
            )
        }
    }
}