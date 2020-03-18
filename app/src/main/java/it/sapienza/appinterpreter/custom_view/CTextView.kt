package it.sapienza.appinterpreter.custom_view

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.setPadding
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.extensions.px
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.view_model.TextView
import it.sapienza.appinterpreter.model.view_model.helper.TextStyle
import org.json.JSONObject
import java.lang.Integer.min


class CTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {

    var view: TextView? = null
    var obj: JSONObject? = null

    init {
        //this.movementMethod = LinkMovementMethod.getInstance()
        ellipsize = TextUtils.TruncateAt.END
    }

    fun configure(view: TextView, orientation: LayoutOrientation, obj: JSONObject?): View? {

        if (view.size != null) {
            layoutParams = LinearLayout.LayoutParams(
                lessThanScreenWidth(view.size?.width?.px) ?: if (orientation == LayoutOrientation.horizontal) {
                    0
                } else {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                },
                view.size?.height?.px ?: ViewGroup.LayoutParams.WRAP_CONTENT,
                if (orientation == LayoutOrientation.horizontal) {
                    if (view.size?.width == null) 1f else 0f
                } else {
                    0f
                }
            )
        } else {
            layoutParams = if (orientation == LayoutOrientation.horizontal) {
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            } else {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
        }



        this.view = view

        fill(obj)

        setPadding(5.px)

        return this
    }

    fun fill(obj: JSONObject?) {
        this.obj = obj
        this.text = this.view?.title
        this.view?.textSize?.px?.let { textSize = it }

        this.view?.textStyle?.let {
            when (it) {
                TextStyle.regular -> {
                    setTypeface(null, Typeface.NORMAL)
                }
                TextStyle.bold -> {
                    setTypeface(null, Typeface.BOLD)
                }
                TextStyle.italic -> {
                    setTypeface(null, Typeface.ITALIC)
                }
            }
        }

        this.view?.mapping?.let {
            this.text = obj?.getValue(it) ?: this.view?.title
        }

        this.view?.action?.let {
            (context.applicationContext as Application).app()?.actionBy(it)
                ?.let { evaluateAction(context, this, it, obj) }
        }

        this.text = this.text.toString().matchReplace(obj)

        this.view?.label?.let {
            val matchReplace = it.matchReplace(obj)
            val span1 =
                SpannableString(matchReplace + if (this.text.isEmpty()) "" else ("\n" + this.text))
            span1.setSpan(
                AbsoluteSizeSpan(20.px),
                0,
                matchReplace.length,
                SPAN_INCLUSIVE_INCLUSIVE
            )
            span1.setSpan(
                ForegroundColorSpan(Color.GRAY),
                0, matchReplace.length,
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE
            )

// let's put both spans together with a separator and all
            // let's put both spans together with a separator and all
            this.text = span1
        }

        Linkify.addLinks(this, Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS)
        linksClickable = true


    }

    private fun lessThanScreenWidth(px: Int?): Int? {
        return px?.let {
            val displayMetrics = DisplayMetrics()
            (context as Activity).getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displayMetrics)
            min(displayMetrics.widthPixels,it)
        }
    }
}