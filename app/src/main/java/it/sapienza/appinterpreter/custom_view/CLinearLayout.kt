package it.sapienza.appinterpreter.custom_view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import it.sapienza.appinterpreter.AppRatioApplication
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.view_model.*
import it.sapienza.appinterpreter.model.view_model.helper.*
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
    }

    fun addCView(view: MView) {
        addView(createAndroidView(view, layout?.orientation, obj, context))
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
                else -> android.view.View(context)
            }
        }
    }

}