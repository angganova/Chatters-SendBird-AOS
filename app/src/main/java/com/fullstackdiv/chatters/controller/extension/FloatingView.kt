package com.fullstackdiv.chatters.controller.extension

import android.widget.PopupWindow
import android.app.Activity
import android.graphics.Point
import android.view.*
import androidx.core.content.ContextCompat
import com.fullstackdiv.chatters.R


/**
 * Created by Angga N P on 4/8/2019.
 */
object FloatingView {
    lateinit var popWindow: PopupWindow

    fun onShowPopup(activity: Activity, inflatedView: View, anchor:View) {

        // get device size
        val display = activity.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        // fill the data to the list items
        // set height depends on the device size
        popWindow = PopupWindow(inflatedView, size.x,
            ViewGroup.LayoutParams.WRAP_CONTENT, true
        )

        // set a background drawable with rounders corners
        popWindow.setBackgroundDrawable(
            ContextCompat.getDrawable(
            activity, R.drawable.vrc_nb_white10
        ))

        // make it focusable to show the keyboard to enter in `EditText`
        popWindow.isFocusable = true
//        popWindow.animationStyle = R.style.attachmentPopupAnimation

        // make it outside touchable to dismiss the popup window
        popWindow.isOutsideTouchable = true

        // show the popup at bottom of the screen and set some margin at
        // bottom ie,
        popWindow.showAtLocation(anchor, Gravity.BOTTOM, 0, 0)
//        popWindow.showAsDropDown(anchor, 8, 8)
    }

    fun dismissWindow() {
        popWindow.dismiss()
    }
}