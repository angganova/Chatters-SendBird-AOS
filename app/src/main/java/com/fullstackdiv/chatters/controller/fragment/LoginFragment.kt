package com.fullstackdiv.chatters.controller.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.controller.activity.MainActivity
import kotlinx.android.synthetic.main.fragment_login.*

/**
 * Created by Angga N P on 3/27/2019.
 */

class LoginFragment: Fragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btLogin.setOnClickListener {
            if (isValid()) (activity as MainActivity).login(etID.text.toString())
        }
    }

    fun isValid(): Boolean{
        return when{
            etID.text.isNullOrBlank() -> {
                (activity as MainActivity).sToast("Empty ID / Email")
                false
            }
            etID.text.length<6 -> {
                (activity as MainActivity).sToast("Invalid ID / Email")
                false
            }
            else -> true
        }
    }
}