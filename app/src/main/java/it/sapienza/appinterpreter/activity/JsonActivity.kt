package it.sapienza.appinterpreter.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.utils.DomainController
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.ui.JsonScreen

class JsonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_Compose)
        setContent {
            AppTheme { JsonScreen(json = DomainController.jsonFile(this), onBack = ::finish) }
        }

    }
}
