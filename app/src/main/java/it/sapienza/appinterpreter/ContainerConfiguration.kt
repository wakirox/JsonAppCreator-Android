package it.sapienza.appinterpreter

import android.content.Context
import android.widget.LinearLayout

import it.sapienza.appinterpreter.custom_view.CLinearLayout
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model.screen.Screen
import it.sapienza.appinterpreter.model.view_model.ButtonView
import it.sapienza.appinterpreter.model.view_model.ImageView
import it.sapienza.appinterpreter.model.view_model.helper.ViewElement

object ContainerConfiguration {

//    fun createContainer(context : Context, result: Screen, parent : LinearLayout) {
//        createContainerObj(context,result)?.let{ it.addTo(parent) }
//    }

    fun createContainer(
        context : Context,
        app : ModelApplication,
        result: Screen,
        parent : LinearLayout
    ){
        result.layouts.map { app.layoutBy(it)!! }.forEach {
            var l = CLinearLayout(context)
            l.configureLayout(it,null,result.dataObj)
            it.views.forEach{
                l.addCView(it)
            }
            parent.addView(l)
        }
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