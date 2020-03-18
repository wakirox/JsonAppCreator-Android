package it.sapienza.appinterpreter.form.view_holder

import android.view.View
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox

import kotlinx.android.synthetic.main.form_item_boolean.view.*
import org.json.JSONObject

class FormItemBooleanViewHolder(view: View) : FormItemViewHolder(view) {



    fun bind(item: CheckBox, obj: JSONObject?) {
        super.bind(item.title)
        setDescription(item.description)

        itemView.switchview.isChecked =
            obj?.let { o -> item.mapping?.let { o.getValue(it)?.toBoolean() } } ?: item.value as Boolean
        itemView.switchview.setOnCheckedChangeListener { _, isChecked -> item.value = isChecked }
    }


}