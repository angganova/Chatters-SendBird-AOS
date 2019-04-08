package com.fullstackdiv.chatters.helper.utils

import android.content.Context
import android.widget.Toast

/**
 * Created by Angga N P on 3/28/2019.
 */


object PopUpUtils {

    private var toast: Toast? = null


    fun sShortToast(ctx: Context, s: String) {
        try {
            toast!!.view.isShown
            toast!!.setText(s)
        } catch (e: Exception) {
            toast = Toast.makeText(ctx, s, Toast.LENGTH_SHORT)
            toast!!.show()
        }

        toast!!.show()
    }

    fun sLongToast(ctx: Context, s: String) {
        try {
            toast!!.view.isShown
            toast!!.setText(s)
        } catch (e: Exception) {
            toast = Toast.makeText(ctx, s, Toast.LENGTH_LONG)
            toast!!.show()
        }

        toast!!.show()
    }
}