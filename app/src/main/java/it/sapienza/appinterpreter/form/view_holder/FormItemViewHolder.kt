package it.sapienza.appinterpreter.form.view_holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.form_item_description.view.*

import kotlinx.android.synthetic.main.form_item_generic.view.*

open class FormItemViewHolder(view : View) : RecyclerView.ViewHolder(view){

    open fun bind(title : String){
        itemView.tv_label.text = title
    }

    fun setDescription(description : String?) {
        description?.let {
            itemView.tv_description.visibility = View.VISIBLE
            itemView.tv_description.text = it
        } ?: run {
            itemView.tv_description.visibility = View.GONE
        }
    }

}