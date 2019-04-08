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
import kotlinx.android.synthetic.main.fragment_base_rv.*


/**
 * Created by Angga N P on 3/27/2019.
 */

class UserListFragment: Fragment(){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_base_rv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAllUser()
    }

    fun getAllUser(){
        // In case of retrieving all users
        val applicationUserListQuery = SendBird.createApplicationUserListQuery()
        applicationUserListQuery.next(UserListQuery.UserListQueryResultHandler { list, e ->
            if (e != null) {
                // Error.
                pb.visibility = View.GONE
                tvEmpty.text = "Error on loading Friend List"
                return@UserListQueryResultHandler
            }else if (activity != null && isAdded) setBaseView(list)
        })
    }

    fun setBaseView(userList: MutableList<User>){
        if (userList.isNotEmpty()) {
            val myPos = myPos(userList)
            if (myPos != null) userList.removeAt(myPos)
            breakObj(userList)

            rv.layoutManager = LinearLayoutManager(this.context)
            rv.itemAnimator = DefaultItemAnimator()

            val adapter = UserAdapter(activity!!, userList, R.layout.item_user_list)
            rv.adapter = adapter
            pb.visibility = View.GONE
        }else {
            pb.visibility = View.GONE
            tvEmpty.text = "User list is empty"
        }
    }

    fun myPos(userList: MutableList<User>) : Int?{
        val userDefault = (activity as MainActivity).userDefault
        for(x in userList.indices) if (userList[x].userId == userDefault.userID) return x
        return null
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