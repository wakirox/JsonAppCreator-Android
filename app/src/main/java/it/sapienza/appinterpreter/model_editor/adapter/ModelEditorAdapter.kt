package it.sapienza.appinterpreter.model_editor.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.appinterpreter.model.ModelApplication
import it.sapienza.appinterpreter.model_editor.viewholder.ClassInfoViewHolder
import it.sapienza.appinterpreter.model_editor.viewholder.general.GenericFieldViewHolder
import kotlin.reflect.full.declaredMemberProperties

enum class ModelAttributeViewType{
    classInfo,
    genericField
}

class ModelEditorAdapter(val app: ModelApplication) : RecyclerView.Adapter<GenericFieldViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericFieldViewHolder {
        when(ModelAttributeViewType.values()[viewType]){
            ModelAttributeViewType.classInfo -> {
                return ClassInfoViewHolder.viewHolder(parent)
            }
            ModelAttributeViewType.genericField -> {
                return GenericFieldViewHolder.viewHolder(parent)
            }
        }
    }

    override fun onBindViewHolder(holder: GenericFieldViewHolder, position: Int) {
        when(ModelAttributeViewType.values()[getItemViewType(position)]){
            ModelAttributeViewType.classInfo -> {
                (holder as ClassInfoViewHolder).bind(app)
            }
            ModelAttributeViewType.genericField -> {
                holder.bind(app::class.declaredMemberProperties.stream().toArray()[position - 1])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(position == 0) return ModelAttributeViewType.classInfo.ordinal
        val field = app::class.declaredMemberProperties.stream().toArray()[position - 1]
        return ModelAttributeViewType.genericField.ordinal

    }

    override fun getItemCount(): Int = 1 + app::class.declaredMemberProperties.size

}