package com.fullstackdiv.chatters.helper.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fullstackdiv.chatters.R
import com.github.chrisbanes.photoview.PhotoView
import com.squareup.picasso.Picasso
import java.io.File


object DialogUtils{

    fun showDialog2(activity:Activity, msg:String, posText: String,
                    posClick: DialogInterface.OnClickListener?,
                    negText: String?= "Cancel"){
        val dialog2 = AlertDialog.Builder(activity)
            .setMessage(msg)
            .setPositiveButton(posText, posClick)
            .setNegativeButton(negText){dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog2.setCanceledOnTouchOutside(false)
        dialog2.setCancelable(false)

        dialog2.show()
    }

    fun sDialogImage(context:Context, str: String? = null,
                     file: File? = null, bitmap: Bitmap? = null){
        val dialog = Dialog(context)

        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_iv)

        val window = dialog.window
        window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val iv  = dialog.findViewById(R.id.iv) as PhotoView

        when {
            str != null -> Glide.with(context).load(str).into(iv)
            file != null -> Glide.with(context).load(file).into(iv)
            else -> iv.setImageBitmap(bitmap)
        }

        dialog.show()
    }
}