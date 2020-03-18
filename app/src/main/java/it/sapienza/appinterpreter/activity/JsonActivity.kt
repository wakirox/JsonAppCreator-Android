package it.sapienza.appinterpreter.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.paulvarry.jsonviewer.JsonViewer
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.utils.DomainController
import org.json.JSONObject

class JsonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_json_viewer)

        val jsonViewLayout = findViewById<JsonViewer>(R.id.jsonViewer)
        jsonViewLayout.setJson(JSONObject(
            DomainController.jsonFile(
                this
            )
        ))
        jsonViewLayout.expandJson()

    }
}