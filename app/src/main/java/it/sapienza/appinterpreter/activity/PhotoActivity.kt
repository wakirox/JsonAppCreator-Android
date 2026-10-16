package it.sapienza.appinterpreter.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import it.sapienza.appinterpreter.ui.AppTheme
import it.sapienza.appinterpreter.ui.PhotoScreen

fun createIntentPhotoActivity(context : Context, url : String) : Intent {
    return Intent(context, PhotoActivity::class.java).apply {
        putExtra("url",url)
    }
}

class PhotoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url")
        setContent {
            AppTheme {
                PhotoScreen(
                    url = url,
                    onBack = ::finish
                )
            }
        }
    }
}
