package com.fullstackdiv.chatters.controller.fragment.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.controller.activity.MainActivity
import com.fullstackdiv.chatters.controller.fragment.main.adapter.UserAdapter
import com.sendbird.android.User
import com.sendbird.android.UserListQuery
import com.sendbird.android.SendBird
import kotlinx.android.synthetic.main.base_rv.*
import com.sendbird.android.GroupChannel


/**
 * Created by Angga N P on 3/27/2019.
 */

class ContactListFragment: Fragment(){
    lateinit var userList: List<User>
    lateinit var adapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.base_rv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAllUser()
    }

    fun getAllUser(){
        // In case of retrieving all users
        val applicationUserListQuery = SendBird.createApplicationUserListQuery()
        applicationUserListQuery.next(UserListQuery.UserListQueryResultHandler { list, e ->
            if (e != null) {    // Error.
                return@UserListQueryResultHandler
            }else{
                userList = list
                setBaseView()
            }
        })
    }

    fun setBaseView(){
        rv.layoutManager = LinearLayoutManager(this.context)
        rv.itemAnimator = DefaultItemAnimator()

        adapter = UserAdapter(activity!!, userList, R.layout.item_user_list)
        rv.adapter = adapter
        adapter.setClickListener(object : UserAdapter.ItemClickListener{
            override fun onClick(view: View, position: Int) {
                val target: MutableList<String> = arrayListOf()
                target.add(0, adapter.data[position].userId)
                (context as MainActivity).openSingleCH(target)
            }
        })

        pb.visibility = View.GONE
    }
}