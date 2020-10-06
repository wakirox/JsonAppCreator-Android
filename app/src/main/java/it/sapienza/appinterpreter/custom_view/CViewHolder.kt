package it.sapienza.appinterpreter.custom_view

import android.app.Application
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.model.action.Action
import org.json.JSONObject

class CViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(
        jsonObject: JSONObject,
        action: Action?,
        lId: Int
    ) {

        val layout : CLinearLayout = this.itemView.findViewById(lId)//(this.itemView as CLinearLayout)
        action?.let {
            (itemView.context.applicationContext as Application)
                .app()!!.actionBy(it)?.event?.let {ev ->
                layout.setOnClickListener {
                    EventManager.evaluateEvent(itemView.context,ev,jsonObject)
                }
            }
        }

        for (i in 0 until layout.getChildCount()) {
            val v : View = layout.getChildAt(i)
            when(v){
                is CTextView -> v.fill(jsonObject)
                is CImageView -> v.fill(jsonObject)
                is CButtonView -> v.fill(jsonObject)
                is CFormView -> v.fill(jsonObject)
                is CListView -> v.fill(jsonObject)
            }
        }

    }


}