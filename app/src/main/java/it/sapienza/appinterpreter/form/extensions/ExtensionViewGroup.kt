package it.sapienza.appinterpreter.form.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

fun ViewGroup.inflate(@LayoutRes resurce : Int) : View {
    return LayoutInflater.from(this.context).inflate(resurce,this,false)
}