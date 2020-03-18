package it.sapienza.appinterpreter.form.view_holder

import android.R
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import kotlinx.android.synthetic.main.form_item_selector.view.*
import org.json.JSONObject

class FormItemSpinnerViewHolder(view : View) : FormItemViewHolder(view) {


    fun bind(item: Selector, obj : JSONObject?) {
        super.bind(item.title)

        val arrayAdapter = ArrayAdapter(itemView.context, R.layout.simple_spinner_item, item.options)
        arrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        itemView.spinner.adapter = arrayAdapter

        item.mapping?.let {
            obj?.getValue(it)?.let { sel ->
                val index = item.options.indexOf(sel)
                if (index > 0) {
                    itemView.spinner.setSelection(index)
                }
            }
        }


        itemView.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                item.selection = item.options[position]
            }
        }

        setDescription(item.description)
    }
}