package it.sapienza.appinterpreter.custom_view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import it.sapienza.androidratio.appratio.R
import it.sapienza.appinterpreter.form.extensions.inflate
import it.sapienza.appinterpreter.form.view_holder.*
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox
import it.sapienza.appinterpreter.model.view_model.form_model.DateInput
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import it.sapienza.appinterpreter.model.view_model.form_model.TextInput
import org.json.JSONObject
import java.util.function.Consumer

class CFormAdapter(
    private val view : FormView,
    private val obj : JSONObject?,
    private val onComplete : Consumer<JSONObject>,
    private val context : Context
) : RecyclerView.Adapter<FormItemViewHolder>(){

    val items = view.formElements

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormItemViewHolder {
        return when(viewType){
            0 -> {
                FormButtonViewHolder(parent.inflate(R.layout.form_item_button))
            }
            1 -> {
               FormItemBooleanViewHolder(parent.inflate(R.layout.form_item_boolean))
            }
            2 -> {
                FormItemStringViewHolder(parent.inflate(R.layout.form_item_string))
            }
//            3 -> {
//                FormItemNumericViewHolder(parent.inflate(R.layout.form_item_string))
//            }
            4 -> {
                FormItemDateViewHolder(parent.inflate(R.layout.form_item_date))
            }
            5 -> FormItemSpinnerViewHolder(parent.inflate(R.layout.form_item_selector))
            else -> {
                FormItemViewHolder(parent.inflate(R.layout.form_item_generic))
            }
        }
    }

    override fun getItemCount(): Int = view.formElements.size + 1

    override fun onBindViewHolder(holder: FormItemViewHolder, position: Int) {
        when(holder){
            is FormButtonViewHolder -> holder.bind(view.buttonTitle,View.OnClickListener {
                if(items.filter { it.isSet() }.size == items.size){
                    onComplete.accept(createObject())
                }else{
                    Toast.makeText(context,"Not all fields are setted",Toast.LENGTH_SHORT).show()
                }
            })
            is FormItemBooleanViewHolder -> holder.bind(items[position] as CheckBox,obj)
            is FormItemStringViewHolder -> holder.bind(items[position] as TextInput,obj)
            is FormItemDateViewHolder -> holder.bind(items[position] as DateInput,obj)
            is FormItemSpinnerViewHolder -> holder.bind(items[position] as Selector,obj)
//            is FormItemNumericViewHolder -> {
//                when(data[position]){
//                    is TextInput -> holder.bind(data[position] as TextInput, obj)
//                    is TextInput -> holder.bind(data[position] as TextInput, obj)
//                }
//            }
        }

    }

    fun createObject() : JSONObject {
        val json = JSONObject("{}")
        items.forEach { elem ->
            elem.mapping?.let {
                if(it.contains(".")){
                    var tmpJson = json
                    val components = it.split(".")
                    for(i in 0 until components.size - 1){
                        if(!tmpJson.has(components[i])){
                            tmpJson.put(components[i],JSONObject("{}"))
                        }
                        tmpJson = tmpJson.getJSONObject(components[i])
                    }
                    tmpJson.put(components.last(),elem.value())
                }else{
                    json.put(it,elem.value())
                }
            }
        }

        return json

    }

    override fun getItemViewType(position: Int): Int {
        if(position >= items.size){
            return 0
        }
        return when(val item = items[position]){
            is CheckBox -> 1
            is TextInput -> 2
            //DataType.Integer, DataType.Number -> 3
            is DateInput -> 4
            is Selector -> 5
            else -> 0
        }
    }

}