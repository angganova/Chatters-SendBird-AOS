package com.fullstackdiv.chatters.controller.activity

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.controller.fragment.main.ChannelListFragment
import com.fullstackdiv.chatters.controller.fragment.main.FriendListFragment
import com.fullstackdiv.chatters.controller.fragment.main.UserListFragment
import com.fullstackdiv.chatters.helper.UserDefault
import com.google.android.material.tabs.TabLayout
import com.sendbird.android.*
import com.sendbird.android.SendBird.ConnectHandler
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var curFragment: Fragment
    lateinit var userDefault: UserDefault

    var opt_menu = 0 // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userDefault = UserDefault(applicationContext)

        toolbar.title = userDefault.nickname
        setSupportActionBar(toolbar)

        setBaseView()
    }

    fun setBaseView(){
        connectServer()
        setAct()
    }

    /*Connection to Server*/
    fun connectServer(){
        SendBird.connect(userDefault.userID, ConnectHandler { user, e ->
            if (e != null) {    // Error.
                e.printStackTrace()
                return@ConnectHandler
            }else{
                cFragmentNoBS(ChannelListFragment())
                println("XXXASDF Logged User ID ${userDefault.userID}")
            }
        })
    }


    fun setAct(){
        fabLogout.setOnClickListener {
            SendBird.disconnect {}
            userDefault.clean()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}
            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabSelected(p0: TabLayout.Tab) {
                when(p0.position){
                    0 -> cFragmentNoBS(ChannelListFragment())
                    1 -> cFragmentNoBS(FriendListFragment())
                    2 -> cFragmentNoBS(UserListFragment())
                }
            }

        })
    }

    fun setSelectionActBar(click: View.OnClickListener){
        if (opt_menu != 0) return

        opt_menu = 1

        toolbar.setNavigationIcon(R.drawable.ic_back_white)
        toolbar.setNavigationOnClickListener(click)
        animColor(ContextCompat.getColor(this, R.color.colorPrimary),
            ContextCompat.getColor(this, R.color.colorPrimaryDark))

        invalidateOptionsMenu()
    }

    fun setNormalActBar(){
        if (opt_menu == 0) return

        opt_menu = 0
        toolbar.title = userDefault.nickname
        animColor(ContextCompat.getColor(this, R.color.colorPrimaryDark),
            ContextCompat.getColor(this, R.color.colorPrimary))

        invalidateOptionsMenu()
        toolbar.navigationIcon = null
    }

    fun updateToolbarMenuCounter(count:Int){
        toolbar.title = count.toString()
        invalidateOptionsMenu()
    }

    fun animColor(colorFrom:Int, colorTo:Int){
        val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnim.duration = 250 // milliseconds
        colorAnim.addUpdateListener { animator ->
            CLMainRoot.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnim.start()
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
//                    println("ASDFXXXX ${groupChannel.url}")
//                    println("ASDFXXXX ${groupChannel.memberCount}")
//                    cFragmentWithBS(ChatDetailFragment().newInstance(groupChannel.url))
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

    /*FRAGMENT CONTAINER FUN*/
    // Change Fragment Without Backstack
    fun cFragmentNoBS(fragment: Fragment) {
        setNormalActBar()

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


    fun hideLO(){
        fabLogout.hide()
    }

    fun showLO(){
        fabLogout.show()
    }

    override fun onBackPressed() {
        if (opt_menu != 0) {
            if(curFragment is ChannelListFragment){
                (curFragment as ChannelListFragment).endSelection()
            }
        }
        else super.onBackPressed()
    }
}
