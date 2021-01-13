package it.sapienza.appinterpreter.custom_view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.util.Consumer
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.appinterpreter.AppRatioApplication
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.extensions.getArray
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.ListOrientation
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.paginatedmanager.ListViewDataTimeViewModel
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject

class CListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    init {
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    var view : ListView? = null
    var obj : MutableList<JSONObject> = mutableListOf()
    var rootObj : JSONObject? = null
//    var PAGE_SIZE = 4
//
//    var isLoading = false
//    var isLastPage = false


    var liveData : ListViewDataTimeViewModel? = null

    fun configure(view : ListView, orientation : LayoutOrientation, obj : JSONObject?): View? {
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        this.view = view

        layoutManager =
            if (view.orientation == ListOrientation.vertical)
                LinearLayoutManager(context)
            else if(view.orientation == ListOrientation.horizontal) {
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }else{
                GridLayoutManager(context,3)
            }

//        addOnScrollListener(recyclerViewOnScrollListener)

        rootObj = obj

        return this
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        fill(rootObj)

    }

    fun fill(obj : JSONObject?){
        visibility = if(view?.isVisible(obj) == true) VISIBLE else GONE
        this.view?.initDataService?.let {
            when(it){
                is RESTService -> {
                    it.paginated?.run {

                        liveData = ListViewDataTimeViewModel(context,view?.mapping!!,it, obj)
                        adapter = CAdapterLive( view!!.action, view!!.orientation,(context.applicationContext as AppRatioApplication).app!!.viewBy(view!!.itemViews[0])!!)
                        liveData?.dataList?.observe(context as MainActivity, { data -> (adapter as CAdapterLive).submitList(data) })

                    } ?: kotlin.run {

//                        EventManager.manage(context,it,obj) { obj ->
//                            (view!!.mapping?.let { obj.getArray(it) }
//                                ?: view!!.dataObjArr)?.let { arr ->
//
//                                for (i in 0 until arr.length()) {
//                                    this.obj.add(arr.getJSONObject(i))
//                                }
//
//                                (context as MainActivity).runOnUiThread {
//                                    val app =
//                                        (context.applicationContext as AppRatioApplication).app!!
//                                    adapter = CMultiAdapter(
//                                        this.obj,
//                                        view!!.action,
//                                        view!!.orientation,
//                                        view!!.viewTypeAttr,
//                                        view!!.itemViews.map { v -> app.viewBy(v)!! }
//                                    )
//                                }
//                            }
//                        }

                    }
                }
            }
        } ?: kotlin.run {






            (view!!.mapping?.let { obj?.getArray(it) } ?: view!!.dataObjArr)?.let {
                for(i in 0 until it.length()){
                    this.obj.add(it.getJSONObject(i))
                }
            }
            val app = (context.applicationContext as AppRatioApplication).app!!
            val items = view!!.itemViews.map { v -> app.viewBy(v)!! }
//            adapter = CAdapter(this.obj, view!!.action, view!!.orientation,(context.applicationContext as AppRatioApplication).app!!.viewBy(view!!.itemViews[0])!!)
            (context as MainActivity).runOnUiThread {

                adapter = CMultiAdapter(
                    this.obj,
                    view!!.action,
                    view!!.orientation,
                    view!!.viewTypeAttr,
                    items
                )
            }
        }
    }



}