package it.sapienza.appinterpreter.model.event

import it.sapienza.appinterpreter.extensions.getValue
import org.json.JSONObject

class PaginatedData(
    val pageMapping : String,
    val totalPageMapping : String) {

    fun getCurrentPage(obj : JSONObject) : Int {
        return  obj.getValue(pageMapping)?.toInt()?.minus(1) ?: obj.getValue("parent_page")?.toInt() ?: 0
    }

    fun getTotalPages(obj: JSONObject) : Int {
        return  obj.getValue(totalPageMapping)!!.toInt()
    }

}