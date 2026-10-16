package it.sapienza.appinterpreter.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.ui.PlainTextScreen

fun spawnInstance(context : Context, changelog : String){
    val intent = Intent(context, PlainTextActivity::class.java)
    intent.putExtra("value",changelog)
    context.startActivity(intent)
}

class PlainTextActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_Compose)
        setContent {
            AppTheme { PlainTextScreen(text = intent.getStringExtra("value").orEmpty(), onBack = ::finish) }
        }

    }

}
