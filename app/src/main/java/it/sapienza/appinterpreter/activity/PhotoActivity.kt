package it.sapienza.appinterpreter.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.extensions.app
import kotlinx.android.synthetic.main.activity_full_screen_image.*

fun createIntentPhotoActivity(context : Context, url : String) : Intent {
    return Intent(context, PhotoActivity::class.java).apply {
        putExtra("url",url)
    }
}

class PhotoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val url = intent.getStringExtra("url")
        Glide.with(this).load(url).into(photoView)

        supportActionBar?.title = application.app()?.name
    }
}