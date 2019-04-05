package com.fullstackdiv.chatters.helper

import android.app.Application
import android.content.Context
import com.fullstackdiv.chatters.R
import com.sendbird.android.SendBird

/**
 * Created by Angga N P on 3/28/2019.
 */


class AppChatters : Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: AppChatters? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()

        SendBird.init(getString(R.string.APP_ID), this)
    }

}