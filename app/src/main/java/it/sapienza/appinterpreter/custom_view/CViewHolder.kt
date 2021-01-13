package it.sapienza.appinterpreter.custom_view

import android.app.Application
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.model.action.Action
import org.json.JSONObject

class CViewHolder(itemView: View,val rootId: Int) : RecyclerView.ViewHolder(itemView) {

    fun bind(
        jsonObject: JSONObject?,
        action: Action?
    ) {

        val layout : CLinearLayout = this.itemView.findViewById(rootId)//(this.itemView as CLinearLayout)
        action?.let {
            (itemView.context.applicationContext as Application)
                .app()!!.actionBy(it)?.event?.let {ev ->
                layout.setOnClickListener {
                    EventManager.evaluateEvent(itemView.context,ev,jsonObject)
                }
            }
        }

        for (i in 0 until layout.childCount) {
            when(val v : View = layout.getChildAt(i)){
                is CTextView -> v.fill(jsonObject)
                is CImageView -> v.fill(jsonObject)
                is CButtonView -> v.fill(jsonObject)
                is CFormView -> v.fill(jsonObject)
                is CListView -> v.fill(jsonObject)
                is CLinearLayout -> v.fill(jsonObject)
                is CViewPager -> v.fill(jsonObject)
            }
        }

    }


}