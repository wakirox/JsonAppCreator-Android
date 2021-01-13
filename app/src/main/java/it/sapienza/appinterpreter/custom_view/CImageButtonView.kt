package it.sapienza.appinterpreter.custom_view

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestListener
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.extensions.px
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.view_model.ButtonView
import kotlinx.android.synthetic.main.plain_text.view.*
import org.json.JSONObject

class CImageButtonView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageButton(context, attrs, androidx.appcompat.R.attr.buttonStyle) {

    var view: ButtonView? = null
    var obj: JSONObject? = null

    fun configure(view: ButtonView, orientation: LayoutOrientation, obj: JSONObject?): View? {

        if(view.size != null){
            val size = view.size!!
            layoutParams = LinearLayout.LayoutParams(
                if(size.width == 0) LinearLayout.LayoutParams.WRAP_CONTENT else size.width.px,
                if(size.height == 0) LinearLayout.LayoutParams.WRAP_CONTENT else size.height.px
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

        this.view?.backgroundColor?.let {
            this.setBackgroundColor(Color.parseColor(it))
        } ?: kotlin.run {
            this.setBackgroundColor(Color.TRANSPARENT)
        }

        Glide.with(this).load(view.image!!).into(this)

        fill(obj)

        return this
    }

    fun fill(
        obj: JSONObject?
    ) {

        visibility = if(view?.isVisible(obj) == true) VISIBLE else GONE

        view?.mapping?.let {
            Glide.with(this).load(obj?.getValue(it) ?: view?.image).into(this)
        }

        this.obj?.let {
            Glide.with(this).load(view?.image?.matchReplace(it)).into(this)
        }

        this.view?.action?.let {
            (context.applicationContext as Application).app()?.actionBy(it)
                ?.let { evaluateAction(context, this, it, obj) }
        }
    }


}