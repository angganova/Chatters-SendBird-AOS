package com.fullstackdiv.chatters.controller.fragment.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.controller.fragment.main.adapter.ChannelAdapter
import kotlinx.android.synthetic.main.base_rv.*
import com.sendbird.android.GroupChannel
import com.sendbird.android.GroupChannelListQuery


/**
 * Created by Angga N P on 3/27/2019.
 */

class ChatListFragment: Fragment(){
    lateinit var channelList: List<GroupChannel>
    lateinit var adapter : ChannelAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.base_rv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAllChannel()
    }

    fun getAllChannel(){
        val channelListQuery = GroupChannel.createMyGroupChannelListQuery()
        channelListQuery.isIncludeEmpty = true
        channelListQuery.next(GroupChannelListQuery.GroupChannelListQueryResultHandler { list, e ->
            if (e != null) {    // Error.
                return@GroupChannelListQueryResultHandler
            }else{
                channelList = list
                setBaseView()
            }
        })
    }

    fun setBaseView(){
        rv.layoutManager = LinearLayoutManager(this.context)
        rv.itemAnimator = DefaultItemAnimator()

        adapter = ChannelAdapter(activity!!, channelList, R.layout.item_channel_list)
        rv.adapter = adapter
        adapter.setClickListener(object : ChannelAdapter.ItemClickListener{
            override fun onClick(view: View, position: Int) {
                val x = adapter.data[position]
                for (field in x.javaClass.declaredFields) {
                    field.isAccessible = true
                    val name = field.name
                    val value = field.get(x)
                    System.out.printf("%s: %s%n", name, value)

                    if (x.lastMessage != null) {
                        for (y in x.lastMessage.javaClass.declaredFields) {
                            y.isAccessible = true
                            System.out.printf("%s: %s%n", y.name, y.get(x.lastMessage))
                        }
                    }

                    if (x.members != null) {
                        for (z in x.members.javaClass.declaredFields) {
                            z.isAccessible = true
                            System.out.printf("%s: %s%n", z.name, z.get(x.members))
                        }
                    }
                }
                println("XXXASDFXXXX \n")
            }

        })
        pb.visibility = View.GONE

        breakObj()
    }

    fun breakObj(){
        for (x in channelList) {
            for (field in x.javaClass.declaredFields) {
                field.isAccessible = true
                val name = field.name
                val value = field.get(x)
                System.out.printf("%s: %s%n", name, value)

                if (x.lastMessage != null) {
                    for (y in x.lastMessage.javaClass.declaredFields) {
                        y.isAccessible = true
                        System.out.printf("%s: %s%n", y.name, y.get(x.lastMessage))
                    }
                }

                if (x.members != null) {
                    for (z in x.members.javaClass.declaredFields) {
                        z.isAccessible = true
                        System.out.printf("%s: %s%n", z.name, z.get(x.members))
                    }
                }
            }
            println("XXXASDFXXXX \n")
        }
    }
}