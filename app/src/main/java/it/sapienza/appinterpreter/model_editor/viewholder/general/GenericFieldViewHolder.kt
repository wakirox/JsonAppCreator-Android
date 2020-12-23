package it.sapienza.appinterpreter.model_editor.viewholder.general

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.extensions.inflate
import it.sapienza.appinterpreter.model_editor.viewholder.ClassInfoViewHolder
import kotlinx.android.synthetic.main.editor_class_info.view.*

open class GenericFieldViewHolder(val view : View) : RecyclerView.ViewHolder(view){
    companion object {
        fun viewHolder(parent: ViewGroup) : GenericFieldViewHolder {
            return GenericFieldViewHolder(parent.inflate(R.layout.editor_class_info,false))
        }
    }

    open fun bind(any: Any) {
        view.tv_class_name.text = "Class name: ${any::class.simpleName}"
    }

}