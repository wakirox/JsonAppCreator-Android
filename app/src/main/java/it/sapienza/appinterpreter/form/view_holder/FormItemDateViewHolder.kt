package it.sapienza.appinterpreter.form.view_holder

import android.app.DatePickerDialog
import android.view.View
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.toDate
import it.sapienza.appinterpreter.form.extensions.stringValue
import it.sapienza.appinterpreter.model.view_model.form_model.DateInput
import kotlinx.android.synthetic.main.form_item_date.view.*
import org.json.JSONObject
import java.util.*

class FormItemDateViewHolder(view : View) : FormItemViewHolder(view){



    fun bind(item: DateInput, obj : JSONObject?) {
        super.bind(item.title)
        setDateString(item)

        val cal = Calendar.getInstance()

        (obj?.let { o -> item.mapping?.let { o.getValue(it) } }
            ?: item.value?.toString())?.let {
            itemView.tv_date.text = it
            it.toDate()?.let { cal.time = it }
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            val instance = Calendar.getInstance()
            instance.set(year,monthOfYear,dayOfMonth)
            item.value = instance.time
            setDateString(item)
        }

        itemView.setOnClickListener {
            DatePickerDialog(itemView.context,
                dateSetListener,
                // set DatePickerDialog to point to today's date when it loads up
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        setDescription(item.description)
    }

    private fun setDateString(item: DateInput) {
        when (val value = item.value) {
            is Date -> itemView.tv_date.text = value.stringValue()
        }
    }

}