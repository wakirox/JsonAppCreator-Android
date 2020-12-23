package it.sapienza.appinterpreter.custom_view

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.activity.createIntentPhotoActivity
import it.sapienza.appinterpreter.extensions.app
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.extensions.px
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.view_model.ImageView
import org.json.JSONObject
import kotlin.math.min


class CImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    var view: ImageView? = null
    var obj: JSONObject? = null



    fun configure(view: ImageView, orientation: LayoutOrientation, obj: JSONObject?): View? {


        if (view.size != null) {
            layoutParams = LinearLayout.LayoutParams(
                lessThanScreenWidth(view.size?.width?.px) ?: if (orientation == LayoutOrientation.horizontal) {
                    0
                } else {
                    ViewGroup.LayoutParams.MATCH_PARENT
                },
                view.size?.height?.px ?: 200.px,
                if (orientation == LayoutOrientation.horizontal) {
                    if (view.size?.width == null) 1f else 0f
                } else {
                    0f
                }
            )
        } else if (orientation == LayoutOrientation.horizontal) {
            layoutParams = LinearLayout.LayoutParams(
                0,
                200.px,
                1f
            )
        } else {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                200.px
            )
        }

        this.view = view
        this.obj = obj

        fill(this.obj)

        setPadding(2.px)



        return this
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

    fun fill(obj: JSONObject?) {
        view?.mapping?.let { obj?.getValue(it) ?: view?.url }?.let { url ->

            Glide.with(this)
                .load(url)
                .centerCrop()
                .into(this)

            this.view!!.action?.let {
                (context.applicationContext as Application).app()?.actionBy(it)
                    ?.let { evaluateAction(context, this, it, obj) }

            } ?: run {
                this.setOnClickListener {
                    context.startActivity(createIntentPhotoActivity(context, url))
                }
            }

        } ?: view?.url?.matchReplace(obj)?.let { url ->

            Glide.with(this)
                .load(url)
                .centerCrop()
                .into(this)

            this.view!!.action?.let {
                (context.applicationContext as Application).app()?.actionBy(it)
                    ?.let { evaluateAction(context, this, it, obj) }
            } ?: run {
                this.setOnClickListener {
                    context.startActivity(createIntentPhotoActivity(context, url))
                }
            }

        } ?: setImageResource(R.drawable.baseline_add_photo_alternate_black_48)


    }
}