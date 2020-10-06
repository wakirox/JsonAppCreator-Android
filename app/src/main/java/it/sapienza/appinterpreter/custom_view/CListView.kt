package it.sapienza.appinterpreter.custom_view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.extensions.getArray
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.view_model.ListView
import org.json.JSONObject
import java.util.function.Consumer

class CListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    init {
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    var view : ListView? = null
    var obj : MutableList<JSONObject> = mutableListOf()

//    var PAGE_SIZE = 4
//
//    var isLoading = false
//    var isLastPage = false

    fun configure(view : ListView, orientation : LayoutOrientation, obj : JSONObject?): View? {
        layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        this.view = view

        layoutManager =
            if (view.orientation == LayoutOrientation.vertical)
                LinearLayoutManager(context)
            else
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

//        addOnScrollListener(recyclerViewOnScrollListener)

        fill(obj)

        return this
    }

    fun fill(obj : JSONObject?){
        this.view?.initData?.let {
            EventManager.manage(context,it,obj, Consumer { obj ->
                (view!!.mapping?.let { obj.getArray(it) } ?: view!!.dataObjArr)?.let {
                    for(i in 0 until it.length()){
                        this.obj.add(it.getJSONObject(i))
                    }
                    (context as MainActivity).runOnUiThread {

//                        this.PAGE_SIZE = this.obj.size - 1

                        adapter = CAdapter(this.obj, view!!.action, view!!.orientation,(context.applicationContext as it.sapienza.appinterpreter.AppRatioApplication).app!!.viewBy(view!!.itemView)!!)
                    }
                }
            })
        } ?: kotlin.run {
            (view!!.mapping?.let { obj?.getArray(it) } ?: view!!.dataObjArr)?.let {
                for(i in 0 until it.length()){
                    this.obj.add(it.getJSONObject(i))
                }
            }
            adapter = CAdapter(this.obj, view!!.action, view!!.orientation,(context.applicationContext as it.sapienza.appinterpreter.AppRatioApplication).app!!.viewBy(view!!.itemView)!!)
        }
    }

    fun loadMoreItems(){

    }

//    private val recyclerViewOnScrollListener: OnScrollListener =
//        object : OnScrollListener() {
//
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//
//                val llmanager = layoutManager as LinearLayoutManager
//
//                val visibleItemCount = llmanager.childCount
//                val totalItemCount = llmanager.itemCount
//                val firstVisibleItemPosition: Int = llmanager.findFirstVisibleItemPosition()
//                if (!isLoading && !isLastPage) {
//                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0 && totalItemCount >= PAGE_SIZE
//                    ) {
//                        loadMoreItems()
//                    }
//                }
//            }
//        }
}