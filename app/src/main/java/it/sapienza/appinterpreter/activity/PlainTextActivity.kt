package it.sapienza.appinterpreter.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.sapienza.androidratio.appratio.R
import kotlinx.android.synthetic.main.plain_text.*

fun spawnInstance(context : Context, changelog : String){
    val intent = Intent(context, PlainTextActivity::class.java)
    intent.putExtra("value",changelog)
    context.startActivity(intent)
}

class PlainTextActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.plain_text)

        title = "Changelog"

        textView.text = intent.getStringExtra("value")

    }

}