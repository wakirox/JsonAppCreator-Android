package it.sapienza.appinterpreter.form.view_holder

import it.sapienza.appinterpreter.form.extensions.afterTextChanged
import it.sapienza.appinterpreter.model.view_model.form_model.TextInput
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType

//class FormItemNumericViewHolder(view : View) : FormItemViewHolder(view) {
//
//    fun bind(item : VirtualProperty) {
//        super.bind(item.name)
//        itemView.edittext.inputType = InputType.TYPE_CLASS_NUMBER
//
//        itemView.edittext.afterTextChanged { item.value = if(item.type == DataType.Integer){it.toDouble()}else{it.toDouble()}}
//        item.value?.let {
//            itemView.edittext.setText(String.format(Locale.ENGLISH,"%f",it))
//        }
//
//    }
//
//    fun bind(item: TextInput, obj : JSONObject?) {
//        super.bind(item.title)
//        itemView.edittext.inputType = InputType.TYPE_CLASS_NUMBER
//
//        itemView.edittext.afterTextChanged { item.value = it}
//        item.value?.let {
//            itemView.edittext.setText(String.format(Locale.ENGLISH,"%f",it))
//        }
//
//        setDescription(item.description)
//
//    }
//
//
//}