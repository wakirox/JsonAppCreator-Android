package it.sapienza.appinterpreter.custom_view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.view_model.*
import it.sapienza.appinterpreter.model.view_model.helper.ViewElement
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
            if(layout.inheritedData == true){
                this.obj = parentData?.let { it } ?: layout.dataObj
            }else{
                this.obj = layout.dataObj?.let { it } ?: parentData
            }
        } ?: run {
            this.obj = parentData //se non è presente nessun dato nel layout eredito di default?
        }

        orientation =
            if (layout.orientation == LayoutOrientation.vertical) VERTICAL else HORIZONTAL
    }

    fun addCView(view: ViewElement) {
        addView(when(view){
            is TextView -> CTextView(context).configure(view,layout?.orientation ?: LayoutOrientation.vertical, obj)
            is ImageView -> CImageView(context).configure(view,layout?.orientation ?: LayoutOrientation.vertical,obj)
            is ButtonView -> CButtonView(context).configure(view,layout?.orientation ?: LayoutOrientation.vertical,obj)
            is ListView -> CListView(context).configure(view,layout?.orientation ?: LayoutOrientation.vertical,obj)
            is FormView -> CFormView(context).configure(view,layout?.orientation ?: LayoutOrientation.vertical,obj)
            else -> View(context)
        })
    }

}