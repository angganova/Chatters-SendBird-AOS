package com.fullstackdiv.chatters.controller.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.fullstackdiv.chatters.*
import com.fullstackdiv.chatters.controller.fragment.ChatDetailFragment
import com.fullstackdiv.chatters.controller.fragment.LoginFragment
import com.fullstackdiv.chatters.controller.fragment.main.ChatListFragment
import com.fullstackdiv.chatters.controller.fragment.main.ContactListFragment
import com.google.android.material.tabs.TabLayout
import com.sendbird.android.*
import com.sendbird.android.SendBird.ConnectHandler
import com.sendbird.android.SendBird.UserInfoUpdateHandler
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import com.sendbird.android.GroupChannel




class MainActivity : AppCompatActivity() {
    private lateinit var curFragment: Fragment
    lateinit var userDefault: UserDefault

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        toolbar.title = ""

        SendBird.init(getString(R.string.APP_ID), this)
        userDefault = UserDefault(this)

        setBaseView()
    }

    fun setBaseView(){
        if (userDefault.isLoggedIn) connectServer()
        else cFragmentNoBS(LoginFragment())

        setAct()
    }

    fun setAct(){
        fabLogout.setOnClickListener {
            userDefault.clean()
            cFragmentNoBS(LoginFragment())
        }

        tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}
            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabSelected(p0: TabLayout.Tab) {
                when(p0.position){
                    0 -> cFragmentNoBS(ChatListFragment())
                    1 -> cFragmentNoBS(ContactListFragment())
                }
            }

        })
    }

    /*Connection to Server*/
    fun connectServer(){
        SendBird.connect(userDefault.userID, ConnectHandler { user, e ->
            if (e != null) {    // Error.
                e.printStackTrace()
                return@ConnectHandler
            }else{
                cFragmentNoBS(ChatListFragment())
                println("XXXASDF Logged User ID ${userDefault.userID}")
            }
        })
    }

    fun disconnectServer(){
        SendBird.disconnect {
            // A current user is disconnected from SendBird server.
        }
    }

    fun login(id: String){
        SendBird.connect(id, ConnectHandler { user, e ->
            if (e != null) {
                // Error.
                cFragmentNoBS(LoginFragment())
                return@ConnectHandler
            }else{
                println("User ID ${user.userId}")
                if (user.nickname.isNullOrBlank()){
                    userDefault.userID = user.userId
                    userDefault.isLoggedIn = true
                    updateProfile(user.userId)
                }else {
                    userDefault.userID = user.userId
                    userDefault.nickname = user.nickname
                    userDefault.isLoggedIn = true
                    cFragmentNoBS(ChatListFragment())
                }
            }
        })
    }

    fun connectWithToken(id:String, token:String){
        SendBird.connect(id, token, ConnectHandler { user, e ->
            if (e != null) {    // Error.
                return@ConnectHandler
            }
        })
    }



    /*Channel Func*/
    fun enterCH(url: String){
        OpenChannel.getChannel(url, OpenChannel.OpenChannelGetHandler { openChannel, e ->
            if (e != null) {    // Error.
                return@OpenChannelGetHandler
            }

            openChannel.enter(OpenChannel.OpenChannelEnterHandler { e ->
                if (e != null) {    // Error.
                    return@OpenChannelEnterHandler
                }
            })
        })
    }

    fun exitCh(url: String){
        OpenChannel.getChannel(url, OpenChannel.OpenChannelGetHandler { openChannel, e ->
            if (e != null) {    // Error.
                return@OpenChannelGetHandler
            }

            openChannel.exit(OpenChannel.OpenChannelExitHandler { e ->
                if (e != null) {    // Error.
                    return@OpenChannelExitHandler
                }
            })
        })

    }

    fun openCH(id:MutableList<String>, distinct:Boolean){
        GroupChannel.createChannelWithUserIds(id, distinct,
            GroupChannel.GroupChannelCreateHandler { groupChannel, e ->
                if (e != null) {
                    // Error.
                    e.printStackTrace()
                    return@GroupChannelCreateHandler
                }else{
                    println("ASDFXXXX ${groupChannel.url}")
                    println("ASDFXXXX ${groupChannel.memberCount}")
                    cFragmentWithBS(ChatDetailFragment().newInstance(groupChannel.url))
                }
            })
    }

    fun openSingleCH(id:MutableList<String>){
        GroupChannel.createChannelWithUserIds(id, true,
            GroupChannel.GroupChannelCreateHandler { groupChannel, e ->
                if (e != null) {
                    // Error.
                    e.printStackTrace()
                    return@GroupChannelCreateHandler
                }else{
                    println("ASDFXXXX ${groupChannel.url}")
                    println("ASDFXXXX ${groupChannel.memberCount}")
                    cFragmentWithBS(ChatDetailFragment().newInstance(groupChannel.url))
                }
            })
    }

    fun deleteCH(){
//        OpenChannel.delete(OpenChannelDeleteHandler { e ->
//            if (e != null) {    // Error.
//                return@OpenChannelDeleteHandler
//            }
//        })
    }

    fun getListCH(){
        val channelListQuery = OpenChannel.createOpenChannelListQuery()
        channelListQuery.next(OpenChannelListQuery.OpenChannelListQueryResultHandler { channels, e ->
            if (e != null) {    // Error.
                return@OpenChannelListQueryResultHandler
            }
        })
    }

    fun getCHByURL(url:String){
        OpenChannel.getChannel(url, OpenChannel.OpenChannelGetHandler { openChannel, e ->
            if (e != null) {    // Error!
                return@OpenChannelGetHandler
            }

            // Successfully fetched the channel.
            // Do something with openChannel.
        })
    }


    /*Message Room*/
    fun sendMsgtoCH(channel: BaseChannel,  msg:String){
        channel.sendUserMessage(msg, BaseChannel.SendUserMessageHandler { userMessage, e ->
            if (e != null) {    // Error.
                return@SendUserMessageHandler
            }
        })
    }

    fun sendData(channel: BaseChannel, msg:String, mData:String){
        channel.sendUserMessage(msg, mData, null, null,
            BaseChannel.SendUserMessageHandler { userMessage, e ->
                if (e != null) {    // Error.
                    return@SendUserMessageHandler
                }
            })
    }

    fun sendFile(channel: BaseChannel, file: File, name:String,
                 type:String, size:Int, mData: String, custom_type:String){
        // Sending file message with raw file
        channel.sendFileMessage(file, name, type, size, mData, custom_type,
            BaseChannel.SendFileMessageHandler { fileMessage, e ->
                if (e != null) {    // Error.
                    return@SendFileMessageHandler
                }
            })
    }

    fun getListMessage(openChannel: BaseChannel){
        val prevMessageListQuery = openChannel.createPreviousMessageListQuery()
        prevMessageListQuery.load(30, true, PreviousMessageListQuery.MessageListQueryResult { messages, e ->
            if (e != null) {    // Error.
                return@MessageListQueryResult
            }
        })
    }

    /*User Action Function*/
    fun blockUser(id:String){
        // In case of blocking a user
        SendBird.blockUserWithUserId(id, SendBird.UserBlockHandler { user, e ->
            if (e != null) {    // Error.
                return@UserBlockHandler
            }
        })

    }

    fun unblockUser(id:String){
        // In case of unblocking a user
        SendBird.unblockUserWithUserId(id, SendBird.UserUnblockHandler { e ->
            if (e != null) {    // Error.
                return@UserUnblockHandler
            }
        })
    }

    fun updateProfile(nickname:String, file: File? =null){
        SendBird.updateCurrentUserInfoWithProfileImage(nickname, file,
            UserInfoUpdateHandler { e ->
                if (e != null) {
                    // Error.
                    sToast("Update user nickname failed")
                    return@UserInfoUpdateHandler
                }else{
                    userDefault.nickname = nickname
                    cFragmentNoBS(ChatListFragment())
                }
            })
    }

    /*FRAGMENT CONTAINER FUN*/
    // Change Fragment Without Backstack
    fun cFragmentNoBS(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.FLC, fragment)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        ft.commit()

        curFragment = fragment
    }

    // Change Fragment With Backstack
    fun cFragmentWithBS(fragment: Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.addToBackStack(null)
        ft.replace(R.id.FLC, fragment)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        ft.commit()

        curFragment = fragment
    }

    // Change Fragment With bundle
    fun cFragmentWithBundle(fragment: Fragment, bundle: Bundle) {
        fragment.arguments = bundle

        val ft = supportFragmentManager.beginTransaction()
        ft.addToBackStack(null)
        ft.replace(R.id.FLC, fragment)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        ft.commit()

        curFragment = fragment
    }

    // Remove Current Fragment
    private fun removeCurrentFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        val currentFrag = supportFragmentManager.findFragmentById(R.id.FLC)
        if (currentFrag != null) transaction.remove(currentFrag)

        transaction.commitAllowingStateLoss()
    }

    // Refresh Fragment
    fun refFragment(fragment: Fragment) {
        val ft = supportFragmentManager.beginTransaction()
        ft.detach(fragment)
        ft.attach(fragment)
        ft.commit()
    }
    /*********************************************************************/

    fun lToast(s: String){
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

    fun sToast(s: String){
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    fun hideLO(){
        fabLogout.hide()
    }

    fun showLO(){
        fabLogout.show()
    }
}
