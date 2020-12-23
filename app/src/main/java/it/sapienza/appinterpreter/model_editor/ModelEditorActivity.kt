package it.sapienza.appinterpreter.model_editor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.model_editor.adapter.ModelEditorAdapter
import kotlinx.android.synthetic.main.activity_model_editor.*

class ModelEditorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_model_editor)

        recycler_model_editor.layoutManager = LinearLayoutManager(this)
        recycler_model_editor.adapter = ModelEditorAdapter(application.app()!!)

    }
}