package com.fullstackdiv.chatters.controller.fragment.main.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.target.SimpleTarget
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.helper.utils.DataUtils
import com.fullstackdiv.chatters.helper.utils.TextUtils
import com.fullstackdiv.chatters.controller.extension.TypingIndicator
import com.sendbird.android.*
import java.util.concurrent.ConcurrentHashMap
import com.fullstackdiv.chatters.helper.utils.FileUtils
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import com.sendbird.android.BaseChannel
import android.util.Base64
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.stfalcon.multiimageview.MultiImageView
import java.io.File
import java.io.IOException


/**
 * Created by Angga N P on 3/27/2019.
 */

class ChannelListAdapter(val context: Context, val rowLayout: Int) : RecyclerView.Adapter<ChannelListAdapter.Holder>() {
    var glide = Glide.with(context)
    var mItemClickListener: OnItemClickListener? = null
    var mItemLongClickListener: OnItemLongClickListener? = null

    var data: MutableList<GroupChannel> = arrayListOf()
    var selectedData = arrayListOf<Boolean>()

    lateinit var target: MutableList<String>

    private val mSimpleTargetIndexMap: ConcurrentHashMap<SimpleTarget<Bitmap>, Int> = ConcurrentHashMap()
    private val mSimpleTargetGroupChannelMap: ConcurrentHashMap<SimpleTarget<Bitmap>, GroupChannel> = ConcurrentHashMap()
    private val mChannelImageNumMap: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
    private val mChannelImageViewMap: ConcurrentHashMap<String, ImageView> = ConcurrentHashMap()
    private val mChannelBitmapMap: ConcurrentHashMap<String, SparseArray<Bitmap>> = ConcurrentHashMap()

    var selection_state = false
    var selection_state_on = false

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        mItemLongClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(channel: GroupChannel, position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(channel: GroupChannel, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(rowLayout, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindView(context, position, data[position],
            mItemClickListener, mItemLongClickListener)
    }


    inner class Holder internal constructor(v: View) : RecyclerView.ViewHolder(v){
        var tvNickname: TextView = v.findViewById(R.id.tvNickname)
        var tvMsg: TextView = v.findViewById(R.id.tvMsg)
        var tvTime: TextView = v.findViewById(R.id.tvTime)
        var tvCount: TextView = v.findViewById(R.id.tvCount)
        var ivPP: ImageView = v.findViewById(R.id.ivPP)
        var mivPP: MultiImageView = v.findViewById(R.id.mivPP)
        var LLIndicator: LinearLayout = v.findViewById(R.id.LLIndicator)

        var ivSelected: ImageView = v.findViewById(R.id.ivSelected)
        var vSelected : View = v.findViewById(R.id.vSelected)

        fun bindView(context: Context, position: Int, channel: GroupChannel,
            clickListener: OnItemClickListener?, longClickListener: OnItemLongClickListener?
        ) {
            when {
                channel.members.size < 2 -> return
                channel.members.size > 2 -> {
                    //Group Channel
                    tvNickname.text = TextUtils.getGroupChannelTitle(channel)
                    setChannelImage(position, channel, mivPP)

                    mivPP.visibility = View.VISIBLE
                    ivPP.visibility = View.GONE
                }
                else -> {
                    tvNickname.text =
                        if (channel.members[0].userId != SendBird.getCurrentUser().userId) channel.members[0].nickname
                        else channel.members[1].nickname

                    glide.load(
                        if (channel.members[0].userId != SendBird.getCurrentUser().userId) channel.members[0].profileUrl
                        else channel.members[1].profileUrl
                    )
                        .override(100, 100)
                        .apply(RequestOptions.circleCropTransform())
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?,
                                                      target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                return false
                            }

                            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?,
                                                         dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                return false
                            }
                        })
                        .into(ivPP)

                    ivPP.visibility = View.VISIBLE
                    mivPP.visibility = View.GONE
                }
            }

            // If there are no unread messages, hide the unread count badge.
            if (channel.unreadMessageCount == 0) tvCount.visibility = View.INVISIBLE
            else {
                tvCount.visibility = View.VISIBLE
                tvCount.text = channel.unreadMessageCount.toString()
            }

            val lastMessage = channel.lastMessage
            if (channel.lastMessage != null) {
                when (lastMessage) {
                    is UserMessage -> tvMsg.text = (channel.lastMessage as UserMessage).message
                    is AdminMessage -> tvMsg.text = (channel.lastMessage as AdminMessage).message
                    else -> {
                        val lastMessageString = String.format(context.getString(R.string.group_channel_list_file_message_text),
                            (lastMessage as FileMessage).sender.nickname)
                        tvMsg.text = lastMessageString
                    }
                }

                tvTime.visibility = View.VISIBLE
                tvMsg.visibility = View.VISIBLE
                tvTime.text = DataUtils.formatDateTime(lastMessage.createdAt)
            }
            else {
                tvTime.visibility = View.INVISIBLE
                tvMsg.visibility = View.INVISIBLE
            }

            // Typing indicator Setting
            val indicatorImages = ArrayList<ImageView>()
            indicatorImages.add(LLIndicator.findViewById(R.id.ind1) as ImageView)
            indicatorImages.add(LLIndicator.findViewById(R.id.ind2) as ImageView)
            indicatorImages.add(LLIndicator.findViewById(R.id.ind3) as ImageView)

            val indicator = TypingIndicator(indicatorImages, 600)
            indicator.animate()

            // If someone in the channel is typing, display the typing indicator.
            if (channel.isTyping) {
                LLIndicator.visibility = View.VISIBLE
                tvMsg.text = if (channel.memberCount>2) "Someone is typing"
                                else "typing . . ."
            } else LLIndicator.visibility = View.GONE


            // Set an OnClickListener to this item.
            if (clickListener != null) itemView.setOnClickListener { clickListener.onItemClick(channel, position) }

            // Set an OnLongClickListener to this item.
            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onItemLongClick(channel, position)
                    true
                }
            }

            // Set Selected
            if (selectedData[position]) {
                ivSelected.visibility = View.VISIBLE
                vSelected.visibility = View.VISIBLE
            }
            else {
                ivSelected.visibility = View.GONE
                vSelected.visibility = View.GONE
            }
        }
    }

    fun updateOrInsert(channel: BaseChannel) {
        if (channel !is GroupChannel) return

        for (i in 0 until data.size) {
            if (data[i].url == channel.url) {
                data.remove(data[i])
                selectedData.removeAt(i)

                data.add(0, channel)
                selectedData.add(0,false)

                notifyDataSetChanged()
                Log.wtf(ChannelListAdapter::class.java.simpleName, "Channel replaced.")
                return
            }
        }

        data.add(0, channel)
        selectedData.add(0,false)

        notifyDataSetChanged()
    }

    fun clearMap() {
        mSimpleTargetIndexMap.clear()
        mSimpleTargetGroupChannelMap.clear()
        mChannelImageNumMap.clear()
        mChannelImageViewMap.clear()
        mChannelBitmapMap.clear()
    }

    fun addChannel(channel: GroupChannel) {
        data.add(channel)
        selectedData.add(false)
        notifyItemInserted(data.size - 1)
    }

    fun removeChannel(i:Int){
        data.remove(data[i])
        selectedData.removeAt(i)
        notifyDataSetChanged()
    }

    fun setChannel(list: MutableList<GroupChannel>){
        data = list
        for (x in data) {
            selectedData.add(false)
        }
        notifyDataSetChanged()
    }

    private fun setChannelImage(position: Int, channel: GroupChannel,
                                multiImageView: MultiImageView
    ) {
        val members = channel.members
        val size = members.size

        if (size >= 1) {
            var imageNum = size
            if (size >= 4) imageNum = 4

            if (!mChannelImageNumMap.containsKey(channel.url)) {
                mChannelImageNumMap[channel.url] = imageNum
                mChannelImageViewMap[channel.url] = multiImageView

                multiImageView.clear()

                for (index in 0 until imageNum) {
                    val simpleTarget = object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val channel = mSimpleTargetGroupChannelMap[this]
                            val index = mSimpleTargetIndexMap[this]
                            if (channel != null && index != null) {
                                var bitmapSparseArray = mChannelBitmapMap[channel.url]
                                if (bitmapSparseArray == null) {
                                    bitmapSparseArray = SparseArray()
                                    mChannelBitmapMap[channel.url] = bitmapSparseArray
                                }
                                bitmapSparseArray.put(index, resource)

                                val num = mChannelImageNumMap[channel.url]
                                if (num != null && num == bitmapSparseArray.size()) {
                                    val multiImageView = mChannelImageViewMap[channel.url] as MultiImageView
                                    for (i in 0 until bitmapSparseArray.size()) {
                                        multiImageView.addImage(bitmapSparseArray.get(i))
                                    }
                                }
                            }
                        }
                    }

                    mSimpleTargetIndexMap[simpleTarget] = index
                    mSimpleTargetGroupChannelMap[simpleTarget] = channel

                    val myOptions = RequestOptions()
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)

                    Glide.with(context)
                        .asBitmap()
                        .load(members[index].profileUrl)
                        .apply(myOptions)
                        .into(simpleTarget)
                }
            } else {
                val bitmapSparseArray = mChannelBitmapMap[channel.url]
                if (bitmapSparseArray != null) {
                    val num = mChannelImageNumMap[channel.url]
                    if (num != null && num == bitmapSparseArray.size()) {
                        multiImageView.clear()
                        for (i in 0 until bitmapSparseArray.size()) {
                            multiImageView.addImage(bitmapSparseArray.get(i))
                        }
                    }
                }
            }
        }
    }

    fun load() {
        try {
            val appDir = File(context.cacheDir, SendBird.getApplicationId())
            appDir.mkdirs()

            val dataFile = File(appDir, TextUtils.generateMD5(
                SendBird.getCurrentUser().userId + "channel_list") + ".data")

            val content = FileUtils.loadFromFile(dataFile)
            val dataArray = content.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            // Reset channel list, then add cached data.
            data.clear()
            selectedData.clear()
            for (i in dataArray.indices) {
                data.add(
                    BaseChannel.buildFromSerializedData(
                        Base64.decode(
                            dataArray[i],
                            Base64.DEFAULT or Base64.NO_WRAP
                        )
                    ) as GroupChannel
                )
                selectedData.add(false)
            }
            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun save() {
        try {
            val sb = StringBuilder()

            // Save the data into file.
            val appDir = File(context.cacheDir, SendBird.getApplicationId())
            appDir.mkdirs()

            val hashFile = File(appDir, TextUtils.generateMD5(
                SendBird.getCurrentUser().userId + "channel_list") + ".hash")
            val dataFile = File(appDir, TextUtils.generateMD5(
                SendBird.getCurrentUser().userId + "channel_list") + ".data")

            if (data.size > 0) {
                // Convert current data into string.
                var channel: GroupChannel?
                for (i in 0 until Math.min(data.size, 100)) {
                    channel = data[i]
                    sb.append("\n")
                    sb.append(Base64.encodeToString(channel.serialize(), Base64.DEFAULT or Base64.NO_WRAP))
                }
                // Remove first newline.
                sb.delete(0, 1)

                val data = sb.toString()
                val md5 = TextUtils.generateMD5(data)

                try {
                    val content = FileUtils.loadFromFile(hashFile)
                    // If data has not been changed, do not save.
                    if (md5 == content) return
                } catch (e: IOException) {
                    // File not found. Save the data.
                    e.printStackTrace()
                }

                FileUtils.saveToFile(dataFile, data)
                FileUtils.saveToFile(hashFile, md5)
            } else {
                FileUtils.deleteFile(dataFile)
                FileUtils.deleteFile(hashFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    // Item Selection
    fun getSelectedChannels():List<GroupChannel>{
        val selectedChannels = arrayListOf<GroupChannel>()
        for (x in selectedData.indices){
            if (selectedData[x]) selectedChannels.add(data[x])
        }

        return selectedChannels
    }

    fun select(pos: Int){
        selection_state = true
        selectedData[pos] = true
        notifyItemChanged(pos)
        selection_state_on = getSelectedState()
    }

    fun unSelect(pos: Int){
        selectedData[pos] = false
        notifyItemChanged(pos)
        selection_state_on = getSelectedState()
    }

    fun isSelected(pos: Int): Boolean{
        return selectedData[pos]
    }

    fun clearSelection(){
        selection_state = false
        selection_state_on = false
        selectedData.clear()
        for (x in data) {
            selectedData.add(false)
        }
        notifyDataSetChanged()
    }

    fun selectedCount():Int{
        var count = 0
        for (x in selectedData){
            if (x) count+=1
        }
        return count
    }

    fun getSelectedState():Boolean {
        for (x in selectedData.indices){
            if (selectedData[x] && data[x].isPushEnabled){
                println("XXX true")
                return true
            }
        }
        println("XXX false")
        return false
    }
}
