package com.fullstackdiv.chatters.helper.utils

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog


object DialogUtils{

    fun showDialog2(activity:Activity, title:String, posText: String,
                    posClick: DialogInterface.OnClickListener?,
                    negText: String?= "Cancel"){
        val dialog2 = AlertDialog.Builder(activity)
            .setMessage(title)
            .setPositiveButton(posText, posClick)
            .setNegativeButton(negText){dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog2.setCanceledOnTouchOutside(false)
        dialog2.setCancelable(false)

        dialog2.show()
    }
}