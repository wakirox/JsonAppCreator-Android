package it.sapienza.appinterpreter.alerts

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.model.event.AlertMessage
import org.json.JSONObject


object AlertUtils {



    fun showAlert(context : Context, text : String, ok : Runnable? = null, ko : Runnable? = null){
        var dialogBuilder = AlertDialog.Builder(context)

        // set message of alert dialog
        dialogBuilder.setMessage(text)
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton("Confirm") { dialog, id -> ok?.run()
            }

        ko?.let {
            dialogBuilder = dialogBuilder.setNegativeButton("Cancel"){
                    dialog, id -> it.run()
            }
        }
            // negative button text and action
//            .setNegativeButton("Cancel", DialogInterface.OnClickListener {
//                    dialog, id -> dialog.cancel()
//            })

        // create dialog box


        (context as Activity).runOnUiThread {
            val alert = dialogBuilder.create()

            alert.setOnShowListener {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Color.BLACK)

                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.BLACK)
            }
            // set title for alert dialog box
            alert.setTitle("Alert")
            // show alert dialog
            alert.show()
        }

    }

    fun showAlert(context: Context, event: AlertMessage, data: JSONObject?, ok : Runnable? = null, ko : Runnable? = null) {
        val message = event.mapping?.let {
            data?.getValue(it)
        } ?: event.value
        showAlert(context,message?.matchReplace(data) ?: "Missing message (check model)", ok,ko)
    }
}