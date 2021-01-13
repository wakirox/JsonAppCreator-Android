package it.sapienza.appinterpreter.utils

import android.app.Activity
import android.content.Context
import it.sapienza.androidratio.appratio.BuildConfig
import it.sapienza.androidratio.appratio.R
import org.json.JSONObject

object DomainController {
    init {

    }

    fun jsonFile(context : Activity) : String {
        return read(R.raw.v3_the_movie_db,context)
//        val sharedPref = context.getSharedPreferences("DomainController",Context.MODE_PRIVATE)
//        return sharedPref.getString("main_file_sav",
//            read(
//                R.raw.v3_app_sample,
//                context
//            )
//        )!!
    }

    fun setJsonFile(value : String, context: Activity){
        val sharedPref = context.getSharedPreferences("DomainController",Context.MODE_PRIVATE)
        val edit = sharedPref.edit()
        edit.putString("main_file_sav", value)
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


    fun saveJson(context: Context, key : String, obj : JSONObject){
        val sharedPref = context.getSharedPreferences("DomainController",Context.MODE_PRIVATE)
        val edit = sharedPref.edit()
        edit.putString("configuration_key_$key", obj.toString())
        edit.apply()
    }

    fun getJson(context: Context, key : String) : JSONObject? {
        val sharedPref = context.getSharedPreferences("DomainController",Context.MODE_PRIVATE)
        return sharedPref.getString("configuration_key_$key",null)?.let {
            JSONObject(it)
        }
    }

}