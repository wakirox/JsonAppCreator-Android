package it.sapienza.appinterpreter

import android.content.Context
import android.widget.LinearLayout

import it.sapienza.appinterpreter.custom_view.CLinearLayout
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model.view_model.helper.MView

object ContainerConfiguration {

//    fun createContainer(context : Context, result: Screen, parent : LinearLayout) {
//        createContainerObj(context,result)?.let{ it.addTo(parent) }
//    }

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

//        result.layouts.map { app.layoutBy(it)!! }.forEach {
//            var l = CLinearLayout(context)
//            l.configureLayout(it,null,result.dataObj)
//            it.views.forEach{
//                l.addCView(it)
//            }
//            parent.addView(l)
//        }
    }
//        when (result) {
//            is ImageView -> {
//                val container = ImageContainer(context)
//                container.configure(result)
//                return container
//            }
//            is TextView -> {
//                val container = TextContainer(context)
//                container.configure(result)
//                return container
//            }
//           is ButtonView -> {
//                val container = ButtonContainer(context)
//                container.configure(result)
//                return container
//            }
//        }
//
//
//
//        return null
//    }

//    fun createContainer(context : Context, result: VirtualObject, parent : LinearLayout) {
//        createContainerObj(context,result)?.let{ it.addTo(parent) }
//    }
//
//    fun createContainerObj(context : Context, result: VirtualObject) : BaseContainer? {
//        when (result.property("type")) {
//            "image" -> {
//                val container = ImageContainer(context)
//                container.configure(result)
//                return container
//            }
//        }
//
//        return null
//    }

}