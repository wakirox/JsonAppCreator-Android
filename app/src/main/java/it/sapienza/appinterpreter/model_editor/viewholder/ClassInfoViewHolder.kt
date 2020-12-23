package it.sapienza.appinterpreter.model_editor.viewholder

import android.view.View
import android.view.ViewGroup
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.extensions.inflate
import it.sapienza.appinterpreter.model_editor.viewholder.general.GenericFieldViewHolder
import kotlinx.android.synthetic.main.editor_class_info.view.*

class ClassInfoViewHolder(view : View) : GenericFieldViewHolder(view){

    companion object {
        fun viewHolder(parent: ViewGroup) : ClassInfoViewHolder {
            return ClassInfoViewHolder(parent.inflate(R.layout.editor_class_info,false))
        }
    }

    override fun bind(obj : Any){
        view.tv_class_name.text = "Class name: ${obj::class.simpleName}"
    }
}