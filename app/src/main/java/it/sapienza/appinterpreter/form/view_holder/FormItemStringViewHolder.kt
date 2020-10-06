package it.sapienza.appinterpreter.form.view_holder

import android.text.InputType
import android.view.View
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.form.extensions.afterTextChanged
import it.sapienza.appinterpreter.model.view_model.form_model.TextInput
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType
import kotlinx.android.synthetic.main.form_item_string.view.*
import org.json.JSONObject

class FormItemStringViewHolder(view: View) : FormItemViewHolder(view) {


    fun bind(item: TextInput, obj: JSONObject?) {
        super.bind(item.title)
        itemView.edittext.afterTextChanged { item.value = it }

        when(item.textType){
            //plain, integer, numeric, email, password
            TextType.numeric -> {
                itemView.edittext.inputType = InputType.TYPE_CLASS_NUMBER
            }
            TextType.email -> {
                itemView.edittext.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            TextType.password -> {
                itemView.edittext.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }

        setDescription(item.description)

        (obj?.let { o -> item.mapping?.let { o.getValue(it) } }
            ?: item.value)?.let {
            itemView.edittext.setText(it)
        }

        item.placeholder?.let {
            itemView.edittext.hint = it
        }

    }
}