package com.fullstackdiv.chatters.controller.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.helper.HelperView
import com.fullstackdiv.chatters.helper.UserDefault
import com.sendbird.android.SendBird
import kotlinx.android.synthetic.main.activity_login.*
import java.io.File

class LoginActivity : AppCompatActivity() {

    val userDefault : UserDefault by lazy { UserDefault(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (userDefault.isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btLogin.setOnClickListener {
            if (isValid()) login(etID.text.toString())
        }
    }

    fun isValid(): Boolean{
        return when{
            etID.text.isNullOrBlank() -> {
                HelperView.sShortToast(this,"Empty ID / Email")
                false
            }
            etID.text.length<6 -> {
                HelperView.sShortToast(this,"Invalid ID / Email")
                false
            }
            else -> true
        }
    }




    fun login(id: String){
        SendBird.connect(id, SendBird.ConnectHandler { user, e ->
            if (e != null) {
                // Error.
                e.printStackTrace()
                return@ConnectHandler
            } else {
                println("User ID ${user.userId}")
                userDefault.userID = user.userId
                userDefault.isLoggedIn = true
                if (user.nickname.isNullOrBlank()) {
                    updateProfile(user.userId)
                } else {
                    userDefault.nickname = user.nickname
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        })
    }


    fun updateProfile(nickname:String, file: File? =null){
        SendBird.updateCurrentUserInfoWithProfileImage(nickname, file,
            SendBird.UserInfoUpdateHandler { e ->
                if (e != null) {
                    // Error.
                    HelperView.sShortToast(this, "Update user nickname failed")
                    return@UserInfoUpdateHandler
                } else {
                    userDefault.nickname = nickname
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            })
    }
}
