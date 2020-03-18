package it.sapienza.appinterpreter.form.view

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import it.sapienza.androidratio.appratio.R

class BooleanView(context: Context) : FrameLayout(context) {

    init {
        val view = LayoutInflater.from(getContext()).inflate(R.layout.form_item_generic,this,true)
        val viewChild= LayoutInflater.from(getContext()).inflate(R.layout.form_item_boolean,this,true)
    }
}