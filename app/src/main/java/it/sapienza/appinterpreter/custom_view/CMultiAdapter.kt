package it.sapienza.appinterpreter.custom_view

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.ListOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.view_model.TextView
import it.sapienza.appinterpreter.model.view_model.helper.MView
import org.json.JSONObject

class CMultiAdapter(
    var data: MutableList<JSONObject>,
    val action: Action?,
    val orientation: ListOrientation,
    val viewTypeAttr : String,
    val views : List<MView>
) : RecyclerView.Adapter<CViewHolder>() {

    val viewIds : MutableList<Int> = mutableListOf()
    init {
        viewIds.add(View.generateViewId())
        viewIds.addAll(views.map { View.generateViewId() })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CViewHolder {
        val l = CLinearLayout(parent.context)

        //l.setBackgroundColor(Color.parseColor("#f6f6f6"))
        if(viewType > 0) {
            val view = views[viewType - 1]
            if (view is Layout) {
                l.configureLayout(view, orientation.toLayout())
            } else {
                val layout = Layout()
                layout.views = mutableListOf(view)
                l.configureLayout(layout, orientation.toLayout())
            }
        }
//
        val param = (l.layoutParams as? LinearLayout.LayoutParams) ?: LinearLayout.LayoutParams(parent.layoutParams)
        param.setMargins(10, 10, 10, 10)
        l.layoutParams = param
//
        when(viewType){
            0 -> {
                l.addCView(TextView(title = "No data available"))
            }
            else -> {
                val view = views[viewType - 1]
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
        l.id = viewIds[viewType]
        cv.addView(l)
        return CViewHolder(cv,l.id)

    }

    override fun getItemViewType(position: Int): Int {
        return  if(data.isEmpty()) 0 else {
            val indexOfFirst =
                views.indexOfFirst { mView -> data[position].has(viewTypeAttr) && data[position].getString(viewTypeAttr) == mView.viewType }
            if(indexOfFirst == -1) 1 else indexOfFirst + 1
        }
    }

    override fun getItemCount(): Int = kotlin.math.max(1, data.size)

    override fun onBindViewHolder(holder: CViewHolder, position: Int) {
        if(getItemViewType(position) > 0) {
//            holder.bind(data[position], views[getItemViewType(position)-1].action ?: action)
        }
    }

}