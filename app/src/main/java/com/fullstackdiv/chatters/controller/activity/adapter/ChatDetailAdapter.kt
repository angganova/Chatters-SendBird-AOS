package com.fullstackdiv.chatters.controller.activity.adapter

import com.sendbird.android.FileMessage
import com.sendbird.android.GroupChannel
import android.content.Context
import android.net.Uri
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sendbird.android.UserMessage
import android.view.ViewGroup
import com.sendbird.android.AdminMessage
import com.sendbird.android.User
import com.sendbird.android.BaseMessage
import com.sendbird.android.BaseChannel
import com.sendbird.android.SendBird
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.helper.utils.DataUtils
import com.fullstackdiv.chatters.helper.utils.ImageUtils
import java.util.*

/**
 * Created by Angga N P on 3/29/2019.
 */

class ChatDetailAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val URL_PREVIEW_CUSTOM_TYPE = "url_preview"

    private val VIEW_MY_TEXT = 99 // My Text Message
    private val VIEW_OTHER_TEXT = 88 // Other Text Message
    private val VIEW_MY_FILE = 77 // My File Type
    private val VIEW_OTHER_FILE = 66 // Other File Type
    private val VIEW_MY_FILE_IMAGE = 55 // My File Image
    private val VIEW_OTHER_FILE_IMAGE = 44 // Other File Image
    private val VIEW_MY_FILE_VIDEO = 33 // My File Video
    private val VIEW_OTHER_FILE_VIDEO = 22 // Other File Video
    private val VIEW_TYPE_ADMIN_MESSAGE = 11 // Admin / System Message

    private var channel: GroupChannel? = null
    private val mFileMessageMap: HashMap<FileMessage, ProgressBar> = HashMap()
    private val data: MutableList<BaseMessage> = ArrayList()

    private var itemClickListener: OnItemClickListener? = null
    private var itemLongClickListener: OnItemLongClickListener? = null

    private val failedMessageList = arrayListOf<String>()
    private val mTempFileMessageUriTable = Hashtable<String, Uri>()
    private var isMessageListLoading: Boolean = false
    private var isSingle: Boolean = true

    private var selectedData = arrayListOf<Boolean>()
    var selection_state = false

    interface OnItemLongClickListener {
        fun onUserMessageItemLongClick(message: UserMessage, position: Int)
        fun onFileMessageItemLongClick(message: FileMessage, position: Int)
        fun onAdminMessageItemLongClick(message: AdminMessage, position: Int)
    }

    interface OnItemClickListener {
        fun onUserMessageItemClick(message: UserMessage, position: Int)
        fun onFileMessageItemClick(message: FileMessage, position: Int)
    }

    override fun getItemCount(): Int { return data.size }

    override fun getItemViewType(position: Int): Int {
        val message = data[position]
//        println("XXXASDF Item Type : ${
//        when(message) {
//            is UserMessage -> "User Message"
//            is FileMessage -> "File Message ${
//            message.type
//            }"
//            else -> "File Message"
//        }
//        }")
        when (message) {
            is UserMessage -> { /* Text Message*/
                return if (message.sender.userId == SendBird.getCurrentUser().userId) VIEW_MY_TEXT
                else VIEW_OTHER_TEXT
            }
            is FileMessage -> return when { /* File Message*/
                message.type.toLowerCase().startsWith("image") ->
                    if (message.sender.userId == SendBird.getCurrentUser().userId) VIEW_MY_FILE_IMAGE
                    else VIEW_OTHER_FILE_IMAGE

                message.type.toLowerCase().startsWith("video") ->
                    if (message.sender.userId == SendBird.getCurrentUser().userId) VIEW_MY_FILE_VIDEO
                    else VIEW_OTHER_FILE_VIDEO

                else -> if (message.sender.userId == SendBird.getCurrentUser().userId) VIEW_MY_FILE
                    else VIEW_OTHER_FILE
            }
            else -> /*Admin Message*/  return VIEW_TYPE_ADMIN_MESSAGE
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = data[position]
        var isContinuous = false
        var isNewDay = false
        var isTempMessage = false
        var isFailedMessage = false
        var tempFileMessageUri: Uri? = null

        // If there is at least one item preceding the current one, check the previous message.
        if (position < data.size - 1) {
            val prevMessage = data[position + 1]

            // If the date of the previous message is different, display the date before the message,
            // and also set isContinuous to false to show information such as the sender's nickname
            // and profile image.
            if (!DataUtils.hasSameDate(message.createdAt, prevMessage.createdAt)) {
                isNewDay = true
                isContinuous = false
            } else isContinuous = isContinuous(message, prevMessage)

        } else if (position == data.size - 1) isNewDay = true

        isTempMessage = isTempMessage(message)
        tempFileMessageUri = getTempFileMessageUri(message)
        isFailedMessage = isFailedMessage(message)

        when (holder.itemViewType) {
            VIEW_MY_TEXT -> (holder as MyUserMessageHolder).bindMyText(
                message as UserMessage, isContinuous, isNewDay, isTempMessage,
                isFailedMessage, itemClickListener, itemLongClickListener, position
            )
            VIEW_OTHER_TEXT -> (holder as OtherUserMessageHolder).bindOthertext(
                message as UserMessage, isNewDay, isContinuous,
                itemClickListener, itemLongClickListener, position
            )
            VIEW_TYPE_ADMIN_MESSAGE -> (holder as AdminMessageHolder).bindAdminMsg(
                message as AdminMessage, isNewDay
            )
            VIEW_MY_FILE -> (holder as MyFileMessageHolder).bindMyFile(
                message as FileMessage, position, isNewDay,
                isTempMessage, isFailedMessage,
                tempFileMessageUri, itemClickListener, itemLongClickListener
            )
            VIEW_OTHER_FILE -> (holder as OtherFileMessageHolder).bindOtherFile(
                message as FileMessage, position, isNewDay,
                isContinuous, itemClickListener, itemLongClickListener
            )
            VIEW_MY_FILE_IMAGE -> (holder as MyImageFileMessageHolder).bindMyImage(
                message as FileMessage, position, isNewDay, isTempMessage, isFailedMessage,
                tempFileMessageUri, itemClickListener, itemLongClickListener
            )
            VIEW_OTHER_FILE_IMAGE -> (holder as OtherImageFileMessageHolder).bindOtherImage(
                message as FileMessage, position, isNewDay, isContinuous, itemClickListener,
                itemLongClickListener
            )
            VIEW_MY_FILE_VIDEO -> (holder as MyVideoFileMessageHolder).bind(
                message as FileMessage, position, isNewDay, isTempMessage,
                isFailedMessage, tempFileMessageUri, itemClickListener,
                itemLongClickListener
            )
            VIEW_OTHER_FILE_VIDEO -> (holder as OtherVideoFileMessageHolder).bindOtherVideo(
                message as FileMessage, position, isNewDay, isContinuous,
                itemClickListener, itemLongClickListener
            )
        }
    }

    fun load(channelUrl: String) {
//        try {
//            val appDir = File(context.cacheDir, SendBird.getApplicationId())
//            appDir.mkdirs()
//
//            val dataFile = File(appDir, TextUtils.generateMD5(SendBird.getCurrentUser().userId + channelUrl) + ".data")
//
//            val content = FileUtils.loadFromFile(dataFile)
//            val dataArray = content.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
//
//            channel = GroupChannel.buildFromSerializedData(Base64.decode(dataArray[0], Base64.DEFAULT or Base64.NO_WRAP)) as GroupChannel?
//
//            // Reset message list, then add cached messages.
//            data.clear()
//            for (i in 1 until dataArray.size) {
//                data.add(
//                    BaseMessage.buildFromSerializedData(
//                        Base64.decode(
//                            dataArray[i],
//                            Base64.DEFAULT or Base64.NO_WRAP
//                        )
//                    )
//                )
//            }
//
//            notifyDataSetChanged()
//        } catch (e: Exception) {
//            // Nothing to load.
//        }
    }

//    fun save() {
//        try {
//            val sb = StringBuilder()
//            if (channel != null) {
//                // Convert current data into string.
//                sb.append(Base64.getEncoder().encodeToString(channel!!.serialize(), Base64.DEFAULT or Base64.NO_WRAP))
//                var message: BaseMessage? = null
//                for (i in 0 until Math.min(data.size, 100)) {
//                    message = data[i]
//                    if (!isTempMessage(message)) {
//                        sb.append("\n")
//                        sb.append(Base64.encodeToString(message.serialize(), Base64.DEFAULT or Base64.NO_WRAP))
//                    }
//                }
//
//                val data = sb.toString()
//                val md5 = TextUtils.generateMD5(data)
//
//                // Save the data into file.
//                val appDir = File(context.cacheDir, SendBird.getApplicationId())
//                appDir.mkdirs()
//
//                val hashFile =
//                    File(appDir, TextUtils.generateMD5(SendBird.getCurrentUser().userId + channel!!.url) + ".hash")
//                val dataFile =
//                    File(appDir, TextUtils.generateMD5(SendBird.getCurrentUser().userId + channel!!.url) + ".data")
//
//                try {
//                    val content = FileUtils.loadFromFile(hashFile)
//                    // If data has not been changed, do not save.
//                    if (md5 == content) {
//                        return
//                    }
//                } catch (e: IOException) {
//                    // File not found. Save the data.
//                }
//
//                FileUtils.saveToFile(dataFile, data)
//                FileUtils.saveToFile(hashFile, md5)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//    }

    /** Inflates the correct layout according to the View Type.**/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            VIEW_MY_TEXT -> return MyUserMessageHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_text_me, parent, false))

            VIEW_OTHER_TEXT -> return OtherUserMessageHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_text_other, parent, false))

            VIEW_TYPE_ADMIN_MESSAGE -> return AdminMessageHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_text_admin, parent, false))

            VIEW_MY_FILE -> return MyFileMessageHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_file_me, parent, false))

            VIEW_OTHER_FILE -> return OtherFileMessageHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_file_other, parent, false))

            VIEW_MY_FILE_IMAGE -> return MyImageFileMessageHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_image_me, parent, false))

            VIEW_OTHER_FILE_IMAGE -> return OtherImageFileMessageHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_image_other, parent, false))

//            VIEW_MY_FILE_VIDEO -> return MyVideoFileMessageHolder(LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_chat_video_me, parent, false))
//
//            VIEW_OTHER_FILE_VIDEO -> return OtherVideoFileMessageHolder(LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_chat_video_other, parent, false))

        }

        return MyUserMessageHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_text_me, parent, false))
    }

    fun setChannel(channel: GroupChannel) {
        this.channel = channel
        isSingle = channel.members.size == 2
    }

    fun isTempMessage(message: BaseMessage?): Boolean {
        return message!!.messageId == 0L
    }

    fun isFailedMessage(message: BaseMessage): Boolean {
        if (!isTempMessage(message)) return false


        if (message is UserMessage) {
            val index = failedMessageList.indexOf(message.requestId)
            if (index >= 0) return true

        } else if (message is FileMessage) {
            val index = failedMessageList.indexOf(message.requestId)
            if (index >= 0) return true
        }

        return false
    }

    fun getTempFileMessageUri(message: BaseMessage): Uri? {
        if (!isTempMessage(message)) return null


        return if (message !is FileMessage) {
            null
        } else mTempFileMessageUriTable.get(message.requestId)

    }

    fun markMessageFailed(requestId: String) {
        failedMessageList.add(requestId)
        notifyDataSetChanged()
    }

    fun removeFailedMessage(message: BaseMessage) {
        if (message is UserMessage) {
            failedMessageList.remove(message.requestId)
            data.remove(message)
        } else if (message is FileMessage) {
            failedMessageList.remove(message.requestId)
            mTempFileMessageUriTable.remove(message.requestId)
            data.remove(message)
        }

        notifyDataSetChanged()
    }

    fun setFileProgressPercent(message: FileMessage, percent: Int) {
        var msg: BaseMessage
        for (i in data.indices.reversed()) {
            msg = data[i]
            if (msg is FileMessage) {
                if (message.requestId == msg.requestId) {
                    val circleProgressBar = mFileMessageMap[message]
                    if (circleProgressBar != null) {
                        circleProgressBar.progress = percent
                    }
                    break
                }
            }
        }
    }

    fun markMessageSent(message: BaseMessage) {
        var msg: BaseMessage
        for (i in data.indices.reversed()) {
            msg = data[i]
            if (message is UserMessage && msg is UserMessage) {
                if (msg.requestId == message.requestId) {
                    data[i] = message
                    notifyDataSetChanged()
                    return
                }
            } else if (message is FileMessage && msg is FileMessage) {
                if (msg.requestId == message.requestId) {
                    mTempFileMessageUriTable.remove(message.requestId)
                    data[i] = message
                    notifyDataSetChanged()
                    return
                }
            }
        }
    }

    fun addTempFileMessageInfo(message: FileMessage, uri: Uri) {
        mTempFileMessageUriTable[message.requestId] = uri
    }

    fun addMessage(message: BaseMessage) {
        data.add(0, message)
        selectedData.add(0,false)
        notifyItemInserted(0)
    }

    fun delete(msgId: Long) {
        for (x in data.indices) {
            if (data[x].messageId == msgId) {
                data.removeAt(x)
                selectedData.removeAt(x)
                notifyItemRemoved(x)
                break
            }
        }
    }

    fun update(message: BaseMessage) {
        var baseMessage: BaseMessage
        for (index in data.indices) {
            baseMessage = data[index]
            if (message.messageId == baseMessage.messageId) {
                data.removeAt(index)
                selectedData.removeAt(index)

                data.add(index, message)
                selectedData.add(0,false)
                notifyDataSetChanged()
                break
            }
        }
    }

    /**
     * Notifies that the user has read all (previously unread) messages in the channel.
     * Typically, this would be called immediately after the user enters the chat and loads
     * its most recent messages.
     */
    fun markAllMessagesAsRead() {
        if (channel != null) channel!!.markAsRead()
        notifyDataSetChanged()
    }

    /**
     * Load old message list.
     * @param limit
     * @param handler
     */
    fun loadPreviousMessages(limit: Int, handler: BaseChannel.GetMessagesHandler?) {
        if (channel == null || isMessageListLoading) return

        var oldestMessageCreatedAt = java.lang.Long.MAX_VALUE
        if (data.size > 0) {
            oldestMessageCreatedAt = data[data.size - 1].createdAt
        }

        isMessageListLoading = true
        channel!!.getPreviousMessagesByTimestamp(oldestMessageCreatedAt,
            false,
            limit,
            true,
            BaseChannel.MessageTypeFilter.ALL,
            null,
            BaseChannel.GetMessagesHandler { list, e ->
                handler?.onResult(list, e)

                isMessageListLoading = false
                if (e != null) {
                    e.printStackTrace()
                    return@GetMessagesHandler
                }

                for (message in list) {
                    data.add(message)
                    selectedData.add(0,false)
                }

                notifyDataSetChanged()
            })
    }

    /**
     * Replaces current message list with new list.
     * Should be used only on initial load or refresh.
     */
    fun loadLatestMessages(limit: Int, handler: BaseChannel.GetMessagesHandler?) {
        if (channel == null) return

        if (isMessageListLoading) return

        val oldestMessageCreatedAt =
            if(data.size > 0) data[data.size - 1].createdAt
            else Long.MAX_VALUE

        isMessageListLoading = true
        channel!!.getPreviousMessagesByTimestamp(oldestMessageCreatedAt,
            true,
            limit,
            true,
            BaseChannel.MessageTypeFilter.ALL,
            null,
            BaseChannel.GetMessagesHandler { list, e ->
                handler?.onResult(list, e)

                println("XXXASDF Message list count ${list.size}")

                isMessageListLoading = false
                if (e != null) {
                    e.printStackTrace()
                    return@GetMessagesHandler
                }

                if (list.size <= 0) return@GetMessagesHandler


                for (message in data) {
                    if (isTempMessage(message) || isFailedMessage(message)) list.add(0, message)
                }

                data.clear()

                for (message in list) {
                    data.add(message)
                    selectedData.add(0,false)
                }

                notifyDataSetChanged()
            })
    }

    fun setItemLongClickListener(listener: OnItemLongClickListener) {
        itemLongClickListener = listener
    }

    fun setItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }

    /**
     * Checks if the current message was sent by the same person that sent the preceding message.
     *
     *
     * This is done so that redundant UI, such as sender nickname and profile picture,
     * does not have to displayed when not necessary.
     */
    private fun isContinuous(currentMsg: BaseMessage?, precedingMsg: BaseMessage?): Boolean {
        // null check
        if (currentMsg == null || precedingMsg == null) {
            return false
        }

        if (currentMsg is AdminMessage && precedingMsg is AdminMessage) {
            return true
        }

        var currentUser: User? = null
        var precedingUser: User? = null

        if (currentMsg is UserMessage) {
            currentUser = currentMsg.sender
        } else if (currentMsg is FileMessage) {
            currentUser = currentMsg.sender
        }

        if (precedingMsg is UserMessage) {
            precedingUser = precedingMsg.sender
        } else if (precedingMsg is FileMessage) {
            precedingUser = precedingMsg.sender
        }

        // If admin message or
        return !(currentUser == null || precedingUser == null) && currentUser.userId == precedingUser.userId

    }

    private inner class AdminMessageHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        private val tvTs: TextView = itemView.findViewById(R.id.tvTs)

        internal fun bindAdminMsg(message: AdminMessage, isNewDay: Boolean) {
            tvBody.text = message.message
            if (isNewDay) {
                tvTs.visibility = View.VISIBLE
                tvTs.text = DataUtils.formatDate(message.createdAt)
            } else tvTs.visibility = View.GONE
        }
    }

    private inner class MyUserMessageHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var CLChat: ConstraintLayout = itemView.findViewById(R.id.CLChat)
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvBody: TextView = itemView.findViewById(R.id.tvBody)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)

        internal fun bindMyText(message: UserMessage, isContinuous: Boolean, isNewDay: Boolean,
                                isTempMessage: Boolean, isFailedMessage: Boolean,
                                clickListener: OnItemClickListener?,
                                longClickListener: OnItemLongClickListener?, position: Int
        ) {

            tvBody.text = message.message

            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = DataUtils.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE

            // If the message is sent on a different date than the previous one, display the date.
            tvTime.text = DataUtils.formatTime(message.createdAt)

            when {
                isFailedMessage -> {
                    ivState.setImageDrawable(this@ChatDetailAdapter.context.getDrawable(R.drawable.ic_send_failed))
                    ivState.visibility = View.VISIBLE
                }
                isTempMessage -> {
                    ivState.setImageDrawable(this@ChatDetailAdapter.context.getDrawable(R.drawable.ic_send_try))
                    ivState.visibility = View.VISIBLE
                }
                else ->
                    // Since setChannel is set slightly after adapter is created
                    if (channel != null) {
                        val readReceipt = channel!!.getReadReceipt(message)
                        ivState.setImageDrawable(this@ChatDetailAdapter.context.getDrawable(R.drawable.ic_send_sent))
                        ivState.visibility = View.VISIBLE
                    }
            }

//            urlPreviewContainer.visibility = View.GONE
//            if (message.customType == URL_PREVIEW_CUSTOM_TYPE) {
//                try {
//                    urlPreviewContainer.visibility = View.VISIBLE
//                    val previewInfo = UrlUtils(message.data)
//                    urlPreviewSiteNameText.text = "@" + previewInfo.siteName
//                    urlPreviewTitleText.setText(previewInfo.title)
//                    urlPreviewDescriptionText.setText(previewInfo.description)
//                    ImageUtils.displayImageFromUrl(context, previewInfo.imageUrl, urlPreviewMainImageView, null)
//                } catch (e: JSONException) {
//                    urlPreviewContainer.visibility = View.GONE
//                    e.printStackTrace()
//                }
//
//            }

            if (clickListener != null) {
                itemView.setOnClickListener { clickListener.onUserMessageItemClick(message, position) }
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onUserMessageItemLongClick(message, position)
                    true
                }
            }

            // Set Selected
            if (selectedData[position]) {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_green_selected)
            } else {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_green)
            }
        }
    }

    private inner class OtherUserMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var CLChat: ConstraintLayout = itemView.findViewById(R.id.CLChat)
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvBody: TextView = itemView.findViewById(R.id.tvBody)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)

        internal fun bindOthertext(message: UserMessage, isNewDay: Boolean, isContinuous: Boolean,
                                   clickListener: OnItemClickListener?,
                                   longClickListener: OnItemLongClickListener?, position: Int) {

            if (channel!!.memberCount>2) tvTitle.text = message.sender.toString()

            tvBody.text = message.message

            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = DataUtils.formatDate(message.createdAt)
            } else {
                tvDate.visibility = View.GONE
            }

            tvTime.text = DataUtils.formatTime(message.createdAt)

            if (clickListener != null) {
                itemView.setOnClickListener { clickListener.onUserMessageItemClick(message, position) }
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onUserMessageItemLongClick(message, position)
                    true
                }
            }

            // Set Selected
            if (selectedData[position]) {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_white_selected)
            } else {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_white)
            }
        }
    }


    /**OTHER FILE VIEW HOLDER**/
    private inner class MyFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var CLChat: ConstraintLayout = itemView.findViewById(R.id.CLChat)
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)

        internal fun bindMyFile(message: FileMessage, position: Int, isNewDay: Boolean,
                                isTempMessage: Boolean, isFailedMessage: Boolean, tempFileMessageUri: Uri?,
                                listener: OnItemClickListener?, longClickListener: OnItemLongClickListener?) {

            tvTitle.text = message.name
            tvTime.text = DataUtils.formatTime(message.createdAt)

            when {
                isFailedMessage -> {
                    ivState.setImageDrawable(this@ChatDetailAdapter.context.getDrawable(R.drawable.ic_send_failed))
                    ivState.visibility = View.VISIBLE
                }
                isTempMessage -> {
                    ivState.setImageDrawable(this@ChatDetailAdapter.context.getDrawable(R.drawable.ic_send_try))
                    ivState.visibility = View.VISIBLE
                }
                else ->
                    // Since setChannel is set slightly after adapter is created
                    if (channel != null) {
                        ivState.setImageDrawable(this@ChatDetailAdapter.context.getDrawable(R.drawable.ic_send_sent))
                        ivState.visibility = View.VISIBLE
                    }
            }

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = DataUtils.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message, position) }
            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onFileMessageItemLongClick(message, position)
                    true
                }
            }

            // Set Selected
            if (selectedData[position]) {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_green_selected)
            } else {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_green)
            }
        }
    }

    private inner class OtherFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var CLChat: ConstraintLayout = itemView.findViewById(R.id.CLChat)
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var tvBody: TextView = itemView.findViewById(R.id.tvBody)
        internal var fileSizeText: TextView? = null
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)

        internal fun bindOtherFile(message: FileMessage, position: Int, isNewDay: Boolean,
                                   isContinuous: Boolean, listener: OnItemClickListener?,
                                   longClickListener: OnItemLongClickListener?) {

            tvTitle.text = message.name
            tvTime.text = DataUtils.formatTime(message.createdAt)

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = DataUtils.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message, position) }
            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onFileMessageItemLongClick(message, position)
                    true
                }
            }


            // Set Selected
            if (selectedData[position]) {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_white_selected)
            } else {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_white)
            }
        }
    }



    /**IMAGES FILE VIEW HOLDER**/
    private inner class MyImageFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var CLChat: ConstraintLayout = itemView.findViewById(R.id.CLChat)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivBody: ImageView = itemView.findViewById(R.id.ivBody)

        internal fun bindMyImage(message: FileMessage, position: Int,
                                 isNewDay: Boolean, isTempMessage: Boolean, isFailedMessage: Boolean,
                                 tempFileMessageUri: Uri?, listener: OnItemClickListener?,
                                 longClickListener: OnItemLongClickListener?) {

            tvTime.text = DataUtils.formatTime(message.createdAt)

            when {
                isFailedMessage -> {
                    ivState.setImageDrawable(this@ChatDetailAdapter.context.getDrawable(R.drawable.ic_send_failed))
                    ivState.visibility = View.VISIBLE
                }
                isTempMessage -> {
                    ivState.setImageDrawable(this@ChatDetailAdapter.context.getDrawable(R.drawable.ic_send_try))
                    ivState.visibility = View.VISIBLE
                }
                else ->
                    // Since setChannel is set slightly after adapter is created
                    if (channel != null) {
                        ivState.setImageDrawable(this@ChatDetailAdapter.context.getDrawable(R.drawable.ic_send_sent))
                        ivState.visibility = View.VISIBLE
                    }
            }

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = DataUtils.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE


            if (isTempMessage && tempFileMessageUri != null) {
                ImageUtils.displayImageFromUrl(this@ChatDetailAdapter.context, tempFileMessageUri.toString(), ivBody, null)
            } else {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<FileMessage.Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    if (message.type.toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(
                            this@ChatDetailAdapter.context,
                            message.url,
                            ivBody,
                            thumbnails[0].url,
                            ivBody.drawable
                        )
                    } else ImageUtils.displayImageFromUrl(this@ChatDetailAdapter.context, thumbnails[0].url, ivBody, ivBody.drawable)
                } else {
                    if (message.type.toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(
                            this@ChatDetailAdapter.context,
                            message.url,
                            ivBody,
                            null as String?,
                            ivBody.drawable
                        )
                    } else ImageUtils.displayImageFromUrl(this@ChatDetailAdapter.context, message.url, ivBody, ivBody.drawable)

                }
            }

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message, position) }
            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onFileMessageItemLongClick(message, position)
                    true
                }
            }

            // Set Selected
            if (selectedData[position]) {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_green_selected)
            } else {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_green)
            }
        }
    }

    private inner class OtherImageFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var CLChat: ConstraintLayout = itemView.findViewById(R.id.CLChat)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivBody: ImageView = itemView.findViewById(R.id.ivBody)

        internal fun bindOtherImage(message: FileMessage, position: Int, isNewDay: Boolean,
                                    isContinuous: Boolean, listener: OnItemClickListener?,
                                    longClickListener: OnItemLongClickListener?) {

            tvTime.text = DataUtils.formatTime(message.createdAt)

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = DataUtils.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE


            if (!isSingle) {
                // Hide profile image and nickname if the previous message was also sent by current sender.
                if (isContinuous) tvTitle.visibility = View.GONE
                else {
                    tvTitle.visibility = View.VISIBLE
                    tvTitle.text = message.sender.nickname
                }
            }else tvTitle.visibility = View.GONE

            // Get thumbnails from FileMessage
            val thumbnails = message.thumbnails as ArrayList<FileMessage.Thumbnail>

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size > 0) {
                if (message.type.toLowerCase().contains("gif")) {
                    ImageUtils.displayGifImageFromUrl(this@ChatDetailAdapter.context, message.url, ivBody, thumbnails[0].url, ivBody.drawable)
                } else {
                    ImageUtils.displayImageFromUrl(this@ChatDetailAdapter.context, thumbnails[0].url, ivBody, ivBody.drawable)
                }
            } else {
                if (message.type.toLowerCase().contains("gif")) {
                    ImageUtils.displayGifImageFromUrl(this@ChatDetailAdapter.context, message.url, ivBody, null as String?, ivBody.drawable)
                } else {
                    ImageUtils.displayImageFromUrl(this@ChatDetailAdapter.context, message.url, ivBody, ivBody.drawable)
                }
            }

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message, position) }
            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onFileMessageItemLongClick(message, position)
                    true
                }
            }

            // Set Selected
            if (selectedData[position]) {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_white_selected)
            } else {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_white)
            }
        }
    }



    /**VIDEOS FILE VIEW HOLDER**/
    private inner class MyVideoFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var CLChat: ConstraintLayout = itemView.findViewById(R.id.CLChat)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivBody: ImageView = itemView.findViewById(R.id.ivBody)

        internal fun bind(message: FileMessage, position: Int, isNewDay: Boolean, isTempMessage: Boolean,
                          isFailedMessage: Boolean, tempFileMessageUri: Uri?,
                          listener: OnItemClickListener?, longClickListener: OnItemLongClickListener?) {

            tvTime.text = DataUtils.formatTime(message.createdAt)
            when {
                isFailedMessage -> {
                    ivState.setImageDrawable(context.getDrawable(R.drawable.ic_send_failed))
                    ivState.visibility = View.VISIBLE
                }
                isTempMessage -> {
                    ivState.setImageDrawable(context.getDrawable(R.drawable.ic_send_try))
                    ivState.visibility = View.VISIBLE
                }
                else ->
                    // Since setChannel is set slightly after adapter is created
                    if (channel != null) {
                        ivState.setImageDrawable(context.getDrawable(R.drawable.ic_send_sent))
                        ivState.visibility = View.VISIBLE
                    }
            }

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = DataUtils.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE


            if (isTempMessage && tempFileMessageUri != null) ImageUtils.displayImageFromUrl(context, tempFileMessageUri.toString(), ivBody, null)
            else {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<FileMessage.Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    ImageUtils.displayImageFromUrl(
                        context,
                        thumbnails[0].url,
                        ivBody,
                        ivBody.drawable
                    )
                }
            }

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message, position) }
            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onFileMessageItemLongClick(message, position)
                    true
                }
            }

            // Set Selected
            if (selectedData[position]) {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_green_selected)
            } else {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_green)
            }
        }
    }

    private inner class OtherVideoFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var CLChat: ConstraintLayout = itemView.findViewById(R.id.CLChat)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivBody: ImageView = itemView.findViewById(R.id.ivBody)

        internal fun bindOtherVideo(message: FileMessage, position: Int, isNewDay: Boolean,
                                    isContinuous: Boolean, listener: OnItemClickListener?,
                                    longClickListener: OnItemLongClickListener?) {

            tvTime.text = DataUtils.formatTime(message.createdAt)

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = DataUtils.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE

            if (!isSingle) {
                // Hide profile image and nickname if the previous message was also sent by current sender.
                if (isContinuous) tvTitle.visibility = View.GONE
                else {
                    ImageUtils.displayRoundImageFromUrl(this@ChatDetailAdapter.context, message.sender.profileUrl, ivBody)

                    tvTitle.visibility = View.VISIBLE
                    tvTitle.text = message.sender.nickname
                }
            }else tvTitle.visibility = View.GONE

            // Get thumbnails from FileMessage
            val thumbnails = message.thumbnails as ArrayList<FileMessage.Thumbnail>

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size > 0) ImageUtils.displayImageFromUrl(this@ChatDetailAdapter.context, thumbnails[0].url, ivBody, ivBody.drawable)

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message, position) }
            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onFileMessageItemLongClick(message, position)
                    true
                }
            }


            // Set Selected
            if (selectedData[position]) {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_white_selected)
            } else {
                CLChat.background = ContextCompat.getDrawable(context, R.drawable.chat_white)
            }
        }
    }


    // Item Selection
    fun select(pos: Int){
        selection_state = true
        selectedData[pos] = true
        notifyItemChanged(pos)
//        selection_state_on = getSelectedState()
    }

    fun unSelect(pos: Int){
        selectedData[pos] = false
        notifyItemChanged(pos)
//        selection_state_on = getSelectedState()
    }

    fun isSelected(pos: Int): Boolean{
        return selectedData[pos]
    }

    fun clearSelection(){
        selection_state = false
        selectedData.clear()
        for (x in data) {
            selectedData.add(false)
        }
        notifyDataSetChanged()
//        selection_state_on = false
    }

    fun selectedCount():Int{
        var count = 0
        for (x in selectedData){
            if (x) count+=1
        }
        return count
    }

    fun getSelectedChat():List<BaseMessage>{
        val selectedChat = arrayListOf<BaseMessage>()
        for (x in selectedData.indices){
            if (selectedData[x]) selectedChat.add(data[x])
        }

        return selectedChat
    }

    fun getSelectedState():Boolean {
//        for (x in selectedData.indices){
//            if (selectedData[x] && data[x].isPushEnabled){
//                println("XXX true")
//                return true
//            }
//        }
//        println("XXX false")
        return false
    }
}

