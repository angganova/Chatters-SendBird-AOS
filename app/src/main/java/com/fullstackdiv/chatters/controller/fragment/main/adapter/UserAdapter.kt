package com.fullstackdiv.chatters.controller.fragment.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.helper.utils.DataUtils
import com.fullstackdiv.chatters.helper.utils.PopUpUtils
import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation



/**
 * Created by Angga N P on 3/27/2019.
 */

class UserAdapter(val context: Context, val data: MutableList<User>, val rowLayout: Int) : RecyclerView.Adapter<UserAdapter.CVHolder>() {
    var picasso = Picasso.with(context)
    private var clickListener: ItemClickListener? = null
    private var longClickListener: LongClickListener? = null

    inner class CVHolder internal constructor(v: View) : RecyclerView.ViewHolder(v), View.OnLongClickListener, View.OnClickListener {
        override fun onLongClick(v: View): Boolean {
            if (longClickListener != null) longClickListener!!.onLong(v, adapterPosition)
            return true
        }

        override fun onClick(v: View) {
            if (clickListener != null) clickListener!!.onClick(v, adapterPosition)
        }

        var tvNickname: TextView = v.findViewById(R.id.tvNickname)
        var tvLastSeen: TextView = v.findViewById(R.id.tvLastSeen)
        var ivPP: ImageView = v.findViewById(R.id.ivPP)
        var ivAdd: ImageView = v.findViewById(R.id.ivAdd)
        var ivOpt: ImageView = v.findViewById(R.id.ivOpt)

        init {
            v.setOnClickListener(this)
            v.setOnLongClickListener(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CVHolder {
        return CVHolder(LayoutInflater.from(parent.context).inflate(rowLayout, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: CVHolder, position: Int) {
        val item = data[position]

        holder.tvNickname.text = item.nickname
        holder.tvLastSeen.text = getLastSeen(item.lastSeenAt)

        println("XXX discovery ${item.friendDiscoveryKey}")
        println("XXX fName ${item.friendName}")
//        if (item.friendDiscoveryKey.isNullOrBlank()){
//            holder.ivAdd.visibility = View.GONE
//        }

        picasso.load(item.profileUrl).
            resize(250, 250).
            centerCrop().
            transform(RoundedCornersTransformation(15, 0)).
            into(holder.ivPP, object : Callback {
                override fun onSuccess() {
                    holder.ivPP.visibility = View.VISIBLE
                }

                override fun onError() {
                    picasso.load(R.drawable.def_img).into(holder.ivPP)
                    holder.ivPP.visibility = View.VISIBLE
                }
            })

        holder.ivAdd.setOnClickListener {
            val friends = arrayListOf<String>()
            friends.add(item.userId)
            addFriend(friends)
        }

        holder.ivOpt.setOnClickListener {
            //creating a popup menu
            val popup = PopupMenu(context, holder.ivOpt)
            //inflating menu from xml resource
            popup.inflate(R.menu.friend_menu)
            //adding click listener
            popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(i: MenuItem): Boolean {
                    return when (i.itemId) {
                        R.id.mnDelete -> {
                            val friends = arrayListOf<String>()
                            friends.add(item.userId)
                            deleteFriend(friends, position)
                            true
                        }
                        R.id.mnReport ->
                            //handle menu2 click
                            false
                        R.id.mnBlock ->
                            //handle menu3 click
                            false
                        else -> false
                    }
                }
            })
            //displaying the popup
            popup.show()
        }
    }

    fun getLastSeen(t: Long): String{
        return "Last seen at ${DataUtils.getDateFromUnix(t)}"
    }

    // User Action
    fun addFriend(friends : ArrayList<String>){
        // Adding friends from ID
        SendBird.addFriends(friends) { list, e ->
            if (e != null) { // Error.
                e.printStackTrace()
                return@addFriends
            } else {
                if (list.size>1) {
                    var string = ""
                    for (x in list) {
                        string += "${x.nickname}, "
                    }
                    PopUpUtils.sShortToast(context, "$string Added")
                }else PopUpUtils.sShortToast(context, "${list[0].nickname} Added")
            }
        }
    }

    fun deleteFriend(friends : ArrayList<String>, pos: Int){
        // In case of deleting friends
        SendBird.deleteFriends(friends) {
            if (it != null) { // Error.
                it.printStackTrace()
                return@deleteFriends
            } else {
                data.removeAt(pos)
                notifyItemRemoved(pos)
                PopUpUtils.sShortToast(context, "Friends deleted")
            }
        }
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


    fun setClickListener(itemClickListener: ItemClickListener) {
        this.clickListener = itemClickListener
    }

    fun setLongClickListener(longClickListener: LongClickListener) {
        this.longClickListener = longClickListener
    }

    interface ItemClickListener {
        fun onClick(view: View, position: Int)
    }

    interface LongClickListener {
        fun onLong(view: View, position: Int)
    }
}
