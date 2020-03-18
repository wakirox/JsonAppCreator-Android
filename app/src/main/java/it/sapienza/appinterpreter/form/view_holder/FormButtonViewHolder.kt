package it.sapienza.appinterpreter.form.view_holder

import android.view.View
import kotlinx.android.synthetic.main.form_item_button.view.*

class FormButtonViewHolder(view : View) : FormItemViewHolder(view) {
    fun bind(title : String?, listener : View.OnClickListener){
        title?.let{
            itemView.btn.text = it
        }
        itemView.btn.setOnClickListener(listener)
    }
}