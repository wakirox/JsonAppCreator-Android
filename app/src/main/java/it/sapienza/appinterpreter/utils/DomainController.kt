package it.sapienza.appinterpreter.utils

import android.app.Activity
import android.content.Context
import it.sapienza.androidratio.appratio.R

object DomainController {
    init {

    }

    fun jsonFile(context : Activity) : String {
        val sharedPref = context.getSharedPreferences("DomainController",Context.MODE_PRIVATE)
        return sharedPref.getString("main_file",
            read(
                R.raw.themoviedb,
                context
            )
        )!!
    }

    fun setJsonFile(value : String, context: Activity){
        val sharedPref = context.getSharedPreferences("DomainController",Context.MODE_PRIVATE)
        val edit = sharedPref.edit()
        edit.putString("main_file", value)
        edit.apply()
    }

    fun setJsonFile(resurce: Int, context: Activity){
        setJsonFile(
            read(
                resurce,
                context
            ),
            context
        )
    }

    private fun read(resurce : Int, context : Activity) : String {
        try {
            val res = context.resources
            val in_s = res.openRawResource(resurce)
            val b = ByteArray(in_s.available())
            in_s.read(b)
            return String(b)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return "{}"
    }

}