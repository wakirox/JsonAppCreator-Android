package it.sapienza.appinterpreter.custom_view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.util.Consumer
import it.sapienza.appinterpreter.AppRatioApplication
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.extensions.getArray
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.event.RESTService
import it.sapienza.appinterpreter.model.view_model.*
import it.sapienza.appinterpreter.model.view_model.helper.*
import it.sapienza.appinterpreter.paginatedmanager.ListViewDataTimeViewModel
import org.json.JSONObject

class CLinearLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var obj : JSONObject? = null
    var layout : Layout? = null

    fun configureLayout(layout : Layout, orientationOut : LayoutOrientation? = LayoutOrientation.vertical, parentData : JSONObject? = null) {
        this.layout = layout

        layoutParams = LayoutParams(
            if(orientationOut != LayoutOrientation.horizontal) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )

        layout.data?.let {
            this.obj = layout.dataObj ?: parentData
        } ?: run {
            this.obj = parentData //se non è presente nessun dato nel layout eredito di default?
        }

        orientation =
            if (layout.orientation == LayoutOrientation.vertical) VERTICAL else HORIZONTAL

        fill(this.obj)
    }

    fun fill(parentData: JSONObject?){

        visibility = if(layout?.isVisible(obj) == true) VISIBLE else GONE

        this.layout?.initDataService?.let {
            when(it){
                is RESTService -> {
                    EventManager.manage(context,it,obj, Consumer { obj ->
                        (layout!!.mapping?.let { obj.getJSONObject(it) } ?: obj).let { data ->
                            for (i in 0 until childCount) {
                                when(val v : View = getChildAt(i)){
                                    is CTextView -> v.fill(data)
                                    is CImageView -> v.fill(data)
                                    is CButtonView -> v.fill(data)
                                    is CFormView -> v.fill(data)
                                    is CListView -> v.fill(data)
                                    is CLinearLayout -> v.fill(data)
                                    is CViewPager -> v.fill(data)
                                }
                            }
                        }
                    })
                }
            }
        } ?: kotlin.run {
            layout?.data?.let {
                this.obj = layout?.dataObj ?: parentData
            } ?: run {
                this.obj = parentData //se non è presente nessun dato nel layout eredito di default?
            }
            for (i in 0 until childCount) {
                when(val v : View = getChildAt(i)){
                    is CTextView -> v.fill(obj)
                    is CImageView -> v.fill(obj)
                    is CButtonView -> v.fill(obj)
                    is CFormView -> v.fill(obj)
                    is CListView -> v.fill(obj)
                    is CLinearLayout -> v.fill(obj)
                    is CViewPager -> v.fill(obj)
                }
            }
        }


    }

    fun addCView(view: MView) {
        addView(createAndroidView( (context.applicationContext as AppRatioApplication).app!!.viewBy(view) ?: view, layout?.orientation, obj, context))
    }

    companion object {
         fun createAndroidView(view: MView, orientation : LayoutOrientation?, obj : JSONObject?, context: Context): android.view.View? {
            return when (view) {
                is Layout -> {

                    val viewBy = (context.applicationContext as AppRatioApplication).app!!.viewBy(view) as Layout
                    var cll = CLinearLayout(context)
                    cll.configureLayout(viewBy,orientation ?: LayoutOrientation.vertical,obj)
                    viewBy.views.forEach{
                        cll.addCView(it)
                    }
                    cll
                }
                is ButtonView -> CButtonView(context).configure(
                        view,
                        orientation ?: LayoutOrientation.vertical,
                        obj
                    )
                is TextView -> CTextView(context).configure(
                    view,
                    orientation ?: LayoutOrientation.vertical,
                    obj
                )
                is ImageView -> CImageView(context).configure(
                    view,
                    orientation ?: LayoutOrientation.vertical,
                    obj
                )
                is ListView -> CListView(context).configure(
                    view,
                    orientation ?: LayoutOrientation.vertical,
                    obj
                )
                is FormView -> CFormView(context).configure(
                    view,
                    orientation ?: LayoutOrientation.vertical,
                    obj
                )
                is PagerView -> CViewPager(context).configure(
                    view,
                    obj
                )
                else -> android.view.View(context)
            }
        }
    }

}