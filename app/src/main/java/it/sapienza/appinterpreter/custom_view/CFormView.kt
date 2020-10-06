package it.sapienza.appinterpreter.custom_view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.appinterpreter.extensions.getArray
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.px
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.TextView
import org.json.JSONObject
import java.util.function.Consumer

class CFormView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    var view : FormView? = null
    var obj : MutableList<JSONObject> = mutableListOf()

    fun configure(view : FormView, orientation : LayoutOrientation, obj : JSONObject?): View? {
        layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        this.view = view

        layoutManager = LinearLayoutManager(context)
        fill(obj)

        return this
    }

    fun fill(obj : JSONObject?){
        adapter = CFormAdapter(view!!, obj,  Consumer {
            EventManager.evaluateEvent(context,view!!.action!!.event!!,it)
        },context)
    }
}