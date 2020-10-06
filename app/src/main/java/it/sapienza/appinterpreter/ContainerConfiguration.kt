package it.sapienza.appinterpreter

import android.content.Context
import android.widget.LinearLayout

import it.sapienza.appinterpreter.custom_view.CLinearLayout
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model.view_model.helper.MView

object ContainerConfiguration {

    fun createContainer(
        context : Context,
        app : ModelApplication,
        result: MView,
        parent : LinearLayout
    ){
        if(result is Layout){
            val viewBy = app.viewBy(result) as Layout
            var l = CLinearLayout(context)
            l.configureLayout(viewBy,null,result.dataObj)
            viewBy.views.forEach{
                l.addCView(it)
            }
            parent.addView(l)
        }else{
            parent.addView(CLinearLayout.createAndroidView(result, LayoutOrientation.vertical, result.dataObj, context))
        }
    }

}