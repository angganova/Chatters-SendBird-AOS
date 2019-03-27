package com.fullstackdiv.chatters.controller.fragment.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fullstackdiv.chatters.R
import com.sendbird.android.GroupChannel
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation

/**
 * Created by Angga N P on 3/27/2019.
 */

class ChannelAdapter(context: Context, val data: List<GroupChannel>, val rowLayout: Int) : RecyclerView.Adapter<ChannelAdapter.CVHolder>() {
    var picasso = Picasso.with(context)
    lateinit var target: MutableList<String>
    private var clickListener: ItemClickListener? = null
    private var longClickListener: LongClickListener? = null

    inner class CVHolder internal constructor(v: View) : RecyclerView.ViewHolder(v), View.OnLongClickListener, View.OnClickListener{
        override fun onLongClick(v: View): Boolean {
            if (longClickListener != null) longClickListener!!.onLong(v, adapterPosition)
            return true
        }

        override fun onClick(v: View) {
            if (clickListener != null) clickListener!!.onClick(v, adapterPosition)
        }

        var tvNickname: TextView = v.findViewById(R.id.tvNickname)
        var tvMsg: TextView = v.findViewById(R.id.tvMsg)
        var ivPP: ImageView = v.findViewById(R.id.ivPP)

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

        for (x in item.members){
            println("Member ID : ${x.userId}")
            println("Member Nicname : ${x.nickname}")
        }
        println("Member : SPACE \n")

        holder.tvNickname.text = item.members[0].nickname
        holder.tvMsg.text = item.lastMessage?.data?:""

        if (item.members[0].profileUrl.isNullOrBlank()) {
            picasso.load(R.drawable.def_img).into(holder.ivPP)
            holder.ivPP.visibility = View.VISIBLE
        } else{
            picasso.load(item.members[0].profileUrl).
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
        }
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
