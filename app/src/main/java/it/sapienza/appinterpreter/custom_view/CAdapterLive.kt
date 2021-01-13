package it.sapienza.appinterpreter.custom_view

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.ListOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.TextView
import it.sapienza.appinterpreter.model.view_model.helper.MView
import org.json.JSONObject

class CAdapterLive(val action: Action?,
                   val orientation: ListOrientation,
                   val view : MView
) : PagedListAdapter<JSONObject, CViewHolder>(DIFF_CALLBACK) {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CViewHolder {
        val l = CLinearLayout(parent.context)

        //l.setBackgroundColor(Color.parseColor("#f6f6f6"))

        if(view is Layout) {
            l.configureLayout(view, orientation.toLayout())
        }else{
            val layout = Layout()
            layout.views = mutableListOf(view)
            l.configureLayout(layout,orientation.toLayout())
        }

        val param = l.layoutParams as LinearLayout.LayoutParams
        param.setMargins(10, 10, 10, 10)
        l.layoutParams = param

        when(viewType){
            0 -> {
                l.addCView(TextView(title = "No data available"))
            }
            1 -> {
                if(view is Layout){
                    view.views.forEach {
                        l.addCView(it)
                    }
                }else{
                    l.addCView(view)
                }

            }
        }

        val cv = CardView(parent.context)
        cv.layoutParams = l.layoutParams
        l.id = View.generateViewId()
        cv.addView(l)
        return CViewHolder(cv, l.id)

    }

    override fun onBindViewHolder(holder: CViewHolder, position: Int) {
            holder.bind( this.getItem(position), action)
    }

    override fun getItemViewType(position: Int): Int {
        return 1
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<JSONObject>(){
            override fun areItemsTheSame(oldItem: JSONObject, newItem: JSONObject): Boolean {
                return oldItem.toString() == newItem.toString()
            }

            override fun areContentsTheSame(oldItem: JSONObject, newItem: JSONObject): Boolean {
                return oldItem.toString() == newItem.toString()
            }
        }
    }
}