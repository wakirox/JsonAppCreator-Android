package it.sapienza.appinterpreter.custom_view

import android.app.Application
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.action.ActionType
import it.sapienza.appinterpreter.model.view_model.ButtonView
import org.json.JSONObject

class CButtonView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatButton(context, attrs, androidx.appcompat.R.attr.buttonStyle) {

    var view: ButtonView? = null
    var obj: JSONObject? = null

    fun configure(view: ButtonView, orientation: LayoutOrientation, obj: JSONObject?): View? {

        if (orientation == LayoutOrientation.horizontal) {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        } else {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        this.view = view
        this.obj = obj

        this.text = view.title

        fill(obj)

        return this
    }

    fun fill(
        obj: JSONObject?
    ) {
        view?.mapping?.let {
            this.text = obj?.getValue(it) ?: view?.title
        }

        this.obj?.let {
            this.text = this.text.toString().matchReplace(it)
        }

        this.view?.action?.let {
            (context.applicationContext as Application).app()?.actionBy(it)
                ?.let { evaluateAction(context, this, it, obj) }
        }
    }


}

fun evaluateAction(
    context: Context,
    view: View,
    action: Action,
    obj: JSONObject?
) {

    val executor = fun(action: Action) {
        val data =
            if (action.inheritedData == true) obj ?: action.dataObj else action.dataObj ?: obj
        action.event?.eventInstance?.let {
            EventManager.evaluateEvent(context, it, data)
        }
    }

    action.let {
        when (it.type) {
            ActionType.click -> {
                view.setOnClickListener { v ->
                    executor(it)
                }
            }
            ActionType.longclick -> {
                view.setOnLongClickListener { v ->
                    executor(it)
                    true
                }
            }
        }
    }
}