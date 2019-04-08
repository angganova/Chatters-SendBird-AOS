package com.fullstackdiv.chatters.controller.fragment.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.controller.activity.ChatDetailActivity
import com.fullstackdiv.chatters.controller.fragment.main.adapter.UserAdapter
import com.sendbird.android.FriendListQuery
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import com.sendbird.android.User
import kotlinx.android.synthetic.main.fragment_base_rv.*


/**
 * Created by Angga N P on 3/27/2019.
 */

class FriendListFragment: Fragment(){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_base_rv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getFriendList()
    }

    fun getFriendList(){
        val friendListQuery = SendBird.createFriendListQuery()
        friendListQuery.next(FriendListQuery.FriendListQueryResultHandler { list, e ->
            if (e != null) {
                // Error.
                e.printStackTrace()
                pb.visibility = View.GONE
                tvEmpty.text = "Error on loading Friend List"
                return@FriendListQueryResultHandler
            }else if (activity != null && isAdded) setBaseView(list)
        })
    }

    fun setBaseView(friendList: MutableList<User>){
        if (friendList.isNotEmpty()) {
            rv.layoutManager = LinearLayoutManager(this.context)
            rv.itemAnimator = DefaultItemAnimator()
            breakObj(friendList)

            val adapter = UserAdapter(activity!!, friendList, R.layout.item_friend_list)
            rv.adapter = adapter
            adapter.setClickListener(object : UserAdapter.ItemClickListener {
                override fun onClick(view: View, position: Int) {
                    val target: MutableList<String> = arrayListOf()
                    target.add(0, adapter.data[position].userId)
                    openSingleCH(target)
                }
            })

            pb.visibility = View.GONE
        }else {
            pb.visibility = View.GONE
            tvEmpty.text = "Friend List is empty"
        }

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
                    val intent = Intent(activity!!, ChatDetailActivity::class.java)
                    intent.putExtra("url", groupChannel.url)
                    startActivity(intent)
                }
            })
    }

    fun breakObj(userList: MutableList<User>){
        for (x in userList) {
            for (field in x.javaClass.declaredFields) {
                field.isAccessible = true
                val name = field.name
                val value = field.get(x)
                System.out.printf("%s: %s%n", name, value)
            }

            println("XXXASDF LINE XXXASDF")
        }
    }
}