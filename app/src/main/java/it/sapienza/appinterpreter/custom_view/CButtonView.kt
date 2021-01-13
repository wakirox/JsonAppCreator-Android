package it.sapienza.appinterpreter.custom_view

import android.R.attr.path
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.extensions.px
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.action.ActionType
import it.sapienza.appinterpreter.model.view_model.ButtonView
import org.json.JSONObject

private const val TAG = "CButtonView"

class CButtonView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatButton(context, attrs, androidx.appcompat.R.attr.buttonStyle) {

    init {
        setPadding(2.px, 2.px, 2.px, 2.px)
    }
    var view: ButtonView? = null
    var obj: JSONObject? = null

    fun configure(view: ButtonView, orientation: LayoutOrientation, obj: JSONObject?): View? {

        if(view.size != null){
            val size = view.size!!
            layoutParams = LinearLayout.LayoutParams(
                if (size.width == 0) LinearLayout.LayoutParams.WRAP_CONTENT else size.width.px,
                if (size.height == 0) LinearLayout.LayoutParams.WRAP_CONTENT else size.height.px
            )
        } else if (orientation == LayoutOrientation.horizontal) {
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

        //https://github.com/bumptech/glide/issues/2923
        if(this.view?.image != null){
            Glide.with(this)
                .load(this.view?.image)
                .centerInside()
                .into(object : CustomTarget<Drawable>(120,60) {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        setButtonImage(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }

        this.text = view.title

        fill(obj)

        return this
    }

    private fun setButtonImage(resource: Drawable) {
        compoundDrawablePadding = 5.px
        setCompoundDrawablesWithIntrinsicBounds(
            resource,
            null,
            null,
            null
        )
        setBackgroundColor(Color.TRANSPARENT)
        setTextColor(Color.BLACK)
    }

    fun fill(
        obj: JSONObject?
    ) {

        visibility = if(view?.isVisible(obj) == true) VISIBLE else GONE

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
        val data = action.dataObj ?: obj
        action.event?.let {
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