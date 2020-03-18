package it.sapienza.appinterpreter.custom_view

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.TextView
import org.json.JSONObject
import java.lang.Integer.max

class CAdapter(
    val data: MutableList<JSONObject>,
    val action: Action?,
    val orientation: LayoutOrientation,
    val layout: Layout
) : RecyclerView.Adapter<CViewHolder>() {

    val lId = View.generateViewId()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CViewHolder {
        val l = CLinearLayout(parent.context)

        //l.setBackgroundColor(Color.parseColor("#f6f6f6"))

        l.configureLayout(layout, orientation)

        val param = l.layoutParams as LinearLayout.LayoutParams
        param.setMargins(10, 10, 10, 10)
        l.layoutParams = param

        when(viewType){
            0 -> {
                l.addCView(TextView(title = "No data available"))
            }
            1 -> {
                layout.views.forEach {
                    l.addCView(it)
                }
            }
        }

        val cv = CardView(parent.context)
        cv.layoutParams = l.layoutParams
        l.id = lId
        cv.addView(l)
        return CViewHolder(cv)

    }

    override fun getItemViewType(position: Int): Int {
        return  if(data.isEmpty()) 0 else 1
    }

    override fun getItemCount(): Int = max(1,data.size)

    override fun onBindViewHolder(holder: CViewHolder, position: Int) {
        if(getItemViewType(position) == 1) {
            holder.bind(data[position], action, lId)
        }
    }

}