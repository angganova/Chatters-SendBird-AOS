package com.fullstackdiv.chatters

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by Angga N P on 3/27/2019.
 */


class UserDefault(private val context: Context) {

    companion object {
        private var mInstance: UserDefault? = null

        // Enc + Dec
        private val FACTORY_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC"
        private val CIPHER_KEY_LEN = 16

        // Shared preferences file name
        private val PREF_NAME = "com.fullstackdiv.chatters"

        //App Setting
        private val LOGIN_SESSION = "login"
        private val ACC_TOKEN = "acc_token"
        private val NICKNAME = "nickname"
        private val USER_ID = "user_id"

        @Synchronized
        fun getInstance(context: Context): UserDefault {
            if (mInstance == null) {
                mInstance = UserDefault(context)
            }
            return mInstance!!
        }
    }


    // Shared Preferences
    private val pref: SharedPreferences
    private val editor: SharedPreferences.Editor

    init {
        this.pref = context.getSharedPreferences(PREF_NAME, 0)
        this.editor = pref.edit()
    }

    var isLoggedIn: Boolean
        get() = pref.getBoolean(LOGIN_SESSION, false)
        set(loggedIn){
            editor.putBoolean(LOGIN_SESSION, loggedIn)
            editor.apply()
        }

    var accToken: String?
        get() = pref.getString(ACC_TOKEN, "0")
        set(token) {
            editor.putString(ACC_TOKEN, token?:"")
            editor.apply()
        }

    var nickname: String?
        get() = pref.getString(NICKNAME, "")
        set(token) {
            editor.putString(NICKNAME, token?:"")
            editor.apply()
        }

    var userID: String?
        get() = pref.getString(USER_ID, "")
        set(token) {
            editor.putString(USER_ID, token?:"")
            editor.apply()
        }

    fun clean() {
        editor.clear()
        editor.apply()
    }

    fun logOut() {
        //Static Setting
        editor.remove(LOGIN_SESSION)
        editor.remove(ACC_TOKEN)
        editor.remove(NICKNAME)

        editor.apply()
    }

}