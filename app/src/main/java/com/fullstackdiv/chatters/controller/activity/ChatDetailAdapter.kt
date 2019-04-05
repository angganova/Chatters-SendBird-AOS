package com.fullstackdiv.chatters.controller.activity

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
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.helper.HelperData
import com.fullstackdiv.chatters.helper.HelperImage
import java.util.*


/**
 * Created by Angga N P on 3/29/2019.
 */

class ChatDetailAdapter(val mContext: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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

    private val mFileMessageMap: HashMap<FileMessage, ProgressBar> = HashMap()
    private var mChannel: GroupChannel? = null
    private val mMessageList: MutableList<BaseMessage> = ArrayList()

    private var mItemClickListener: OnItemClickListener? = null
    private var mItemLongClickListener: OnItemLongClickListener? = null

    private val mFailedMessageIdList = arrayListOf<String>()
    private val mTempFileMessageUriTable = Hashtable<String, Uri>()
    private var isMessageListLoading: Boolean = false
    private var isSingle: Boolean = true

    interface OnItemLongClickListener {
        fun onUserMessageItemLongClick(message: UserMessage, position: Int)
        fun onFileMessageItemLongClick(message: FileMessage)
        fun onAdminMessageItemLongClick(message: AdminMessage)
    }

    interface OnItemClickListener {
        fun onUserMessageItemClick(message: UserMessage)
        fun onFileMessageItemClick(message: FileMessage)
    }

    override fun getItemCount(): Int { return mMessageList.size }

    override fun getItemViewType(position: Int): Int {
        val message = mMessageList[position]

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
        val message = mMessageList[position]
        var isContinuous = false
        var isNewDay = false
        var isTempMessage = false
        var isFailedMessage = false
        var tempFileMessageUri: Uri? = null

        // If there is at least one item preceding the current one, check the previous message.
        if (position < mMessageList.size - 1) {
            val prevMessage = mMessageList[position + 1]

            // If the date of the previous message is different, display the date before the message,
            // and also set isContinuous to false to show information such as the sender's nickname
            // and profile image.
            if (!HelperData.hasSameDate(message.createdAt, prevMessage.createdAt)) {
                isNewDay = true
                isContinuous = false
            } else isContinuous = isContinuous(message, prevMessage)

        } else if (position == mMessageList.size - 1) isNewDay = true

        isTempMessage = isTempMessage(message)
        tempFileMessageUri = getTempFileMessageUri(message)
        isFailedMessage = isFailedMessage(message)

        when (holder.itemViewType) {
            VIEW_MY_TEXT -> (holder as MyUserMessageHolder).bind(
                mContext, message as UserMessage, mChannel,
                isContinuous, isNewDay, isTempMessage, isFailedMessage,
                mItemClickListener, mItemLongClickListener, position
            )
            VIEW_OTHER_TEXT -> (holder as OtherUserMessageHolder).bind(
                mContext, message as UserMessage,
                mChannel, isNewDay, isContinuous,
                mItemClickListener, mItemLongClickListener, position
            )
            VIEW_TYPE_ADMIN_MESSAGE -> (holder as AdminMessageHolder).bind(
                mContext, message as AdminMessage,
                mChannel, isNewDay
            )
            VIEW_MY_FILE -> (holder as MyFileMessageHolder).bind(
                mContext, message as FileMessage,
                mChannel, isNewDay,
                isTempMessage, isFailedMessage,
                tempFileMessageUri, mItemClickListener
            )
            VIEW_OTHER_FILE -> (holder as OtherFileMessageHolder).bind(
                mContext, message as FileMessage,
                mChannel, isNewDay,
                isContinuous, mItemClickListener
            )
            VIEW_MY_FILE_IMAGE -> (holder as MyImageFileMessageHolder).bind(
                mContext, message as FileMessage, mChannel,
                isNewDay, isTempMessage, isFailedMessage,
                tempFileMessageUri, mItemClickListener
            )
            VIEW_OTHER_FILE_IMAGE -> (holder as OtherImageFileMessageHolder).bind(
                mContext, message as FileMessage, mChannel,
                isNewDay, isContinuous, mItemClickListener
            )
            VIEW_MY_FILE_VIDEO -> (holder as MyVideoFileMessageHolder).bind(
                mContext, message as FileMessage, mChannel, isNewDay,
                isTempMessage, isFailedMessage, tempFileMessageUri, mItemClickListener
            )
            VIEW_OTHER_FILE_VIDEO -> (holder as OtherVideoFileMessageHolder).bind(
                mContext, message as FileMessage, mChannel,
                isNewDay, isContinuous, mItemClickListener
            )
        }
    }

    fun load(channelUrl: String) {
//        try {
//            val appDir = File(mContext.cacheDir, SendBird.getApplicationId())
//            appDir.mkdirs()
//
//            val dataFile = File(appDir, TextUtils.generateMD5(SendBird.getCurrentUser().userId + channelUrl) + ".data")
//
//            val content = FileUtils.loadFromFile(dataFile)
//            val dataArray = content.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
//
//            mChannel = GroupChannel.buildFromSerializedData(Base64.decode(dataArray[0], Base64.DEFAULT or Base64.NO_WRAP)) as GroupChannel?
//
//            // Reset message list, then add cached messages.
//            mMessageList.clear()
//            for (i in 1 until dataArray.size) {
//                mMessageList.add(
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
//            if (mChannel != null) {
//                // Convert current data into string.
//                sb.append(Base64.getEncoder().encodeToString(mChannel!!.serialize(), Base64.DEFAULT or Base64.NO_WRAP))
//                var message: BaseMessage? = null
//                for (i in 0 until Math.min(mMessageList.size, 100)) {
//                    message = mMessageList[i]
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
//                val appDir = File(mContext.cacheDir, SendBird.getApplicationId())
//                appDir.mkdirs()
//
//                val hashFile =
//                    File(appDir, TextUtils.generateMD5(SendBird.getCurrentUser().userId + mChannel!!.url) + ".hash")
//                val dataFile =
//                    File(appDir, TextUtils.generateMD5(SendBird.getCurrentUser().userId + mChannel!!.url) + ".data")
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

    /**
     * Inflates the correct layout according to the View Type.
     */
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
        mChannel = channel
        isSingle = channel.members.size == 2
    }

    fun isTempMessage(message: BaseMessage?): Boolean {
        return message!!.messageId == 0L
    }

    fun isFailedMessage(message: BaseMessage): Boolean {
        if (!isTempMessage(message)) return false


        if (message is UserMessage) {
            val index = mFailedMessageIdList.indexOf(message.requestId)
            if (index >= 0) return true

        } else if (message is FileMessage) {
            val index = mFailedMessageIdList.indexOf(message.requestId)
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
        mFailedMessageIdList.add(requestId)
        notifyDataSetChanged()
    }

    fun removeFailedMessage(message: BaseMessage) {
        if (message is UserMessage) {
            mFailedMessageIdList.remove(message.requestId)
            mMessageList.remove(message)
        } else if (message is FileMessage) {
            mFailedMessageIdList.remove(message.requestId)
            mTempFileMessageUriTable.remove(message.requestId)
            mMessageList.remove(message)
        }

        notifyDataSetChanged()
    }

    fun setFileProgressPercent(message: FileMessage, percent: Int) {
        var msg: BaseMessage
        for (i in mMessageList.indices.reversed()) {
            msg = mMessageList[i]
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
        for (i in mMessageList.indices.reversed()) {
            msg = mMessageList[i]
            if (message is UserMessage && msg is UserMessage) {
                if (msg.requestId == message.requestId) {
                    mMessageList[i] = message
                    notifyDataSetChanged()
                    return
                }
            } else if (message is FileMessage && msg is FileMessage) {
                if (msg.requestId == message.requestId) {
                    mTempFileMessageUriTable.remove(message.requestId)
                    mMessageList[i] = message
                    notifyDataSetChanged()
                    return
                }
            }
        }
    }

    fun addTempFileMessageInfo(message: FileMessage, uri: Uri) {
        mTempFileMessageUriTable.put(message.requestId, uri)
    }

    fun addMessage(message: BaseMessage) {
        mMessageList.add(0, message)
        notifyItemInserted(0)
    }

    fun delete(msgId: Long) {
        for (msg in mMessageList) {
            if (msg.messageId == msgId) {
                mMessageList.remove(msg)
                notifyDataSetChanged()
                break
            }
        }
    }

    fun update(message: BaseMessage) {
        var baseMessage: BaseMessage
        for (index in mMessageList.indices) {
            baseMessage = mMessageList[index]
            if (message.messageId == baseMessage.messageId) {
                mMessageList.removeAt(index)
                mMessageList.add(index, message)
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
        if (mChannel != null) mChannel!!.markAsRead()
        notifyDataSetChanged()
    }

    /**
     * Load old message list.
     * @param limit
     * @param handler
     */
    fun loadPreviousMessages(limit: Int, handler: BaseChannel.GetMessagesHandler?) {
        if (mChannel == null || isMessageListLoading) return

        var oldestMessageCreatedAt = java.lang.Long.MAX_VALUE
        if (mMessageList.size > 0) {
            oldestMessageCreatedAt = mMessageList[mMessageList.size - 1].createdAt
        }

        isMessageListLoading = true
        mChannel!!.getPreviousMessagesByTimestamp(oldestMessageCreatedAt,
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
                    mMessageList.add(message)
                }

                notifyDataSetChanged()
            })
    }

    /**
     * Replaces current message list with new list.
     * Should be used only on initial load or refresh.
     */
    fun loadLatestMessages(limit: Int, handler: BaseChannel.GetMessagesHandler?) {
        if (mChannel == null) return

        if (isMessageListLoading) return

        val oldestMessageCreatedAt =
            if(mMessageList.size > 0) mMessageList[mMessageList.size - 1].createdAt
            else Long.MAX_VALUE

        isMessageListLoading = true
        mChannel!!.getPreviousMessagesByTimestamp(oldestMessageCreatedAt,
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


                for (message in mMessageList) {
                    if (isTempMessage(message) || isFailedMessage(message)) list.add(0, message)
                }

                mMessageList.clear()

                for (message in list) {
                    mMessageList.add(message)
                }

                notifyDataSetChanged()
            })
    }

    fun setItemLongClickListener(listener: OnItemLongClickListener) {
        mItemLongClickListener = listener
    }

    fun setItemClickListener(listener: OnItemClickListener) {
        mItemClickListener = listener
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

//
    private inner class AdminMessageHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBody: TextView = itemView.findViewById(R.id.tvBody)
        private val tvTs: TextView = itemView.findViewById(R.id.tvTs)

        internal fun bind(context: Context?, message: AdminMessage, channel: GroupChannel?, isNewDay: Boolean) {
            tvBody.text = message.message
            if (isNewDay) {
                tvTs.visibility = View.VISIBLE
                tvTs.text = HelperData.formatDate(message.createdAt)
            } else tvTs.visibility = View.GONE
        }
    }

    private inner class MyUserMessageHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvBody: TextView = itemView.findViewById(R.id.tvBody)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)
//        internal var editedText: TextView = itemView.findViewById(R.id.text_group_chat_edited)
//        internal var urlPreviewContainer: ViewGroup = itemView.findViewById(R.id.url_preview_container)
//        internal var urlPreviewSiteNameText: TextView = itemView.findViewById(R.id.text_url_preview_site_name)
//        internal var urlPreviewTitleText: TextView = itemView.findViewById(R.id.text_url_preview_title)
//        internal var urlPreviewDescriptionText: TextView = itemView.findViewById(R.id.text_url_preview_description)
//        internal var urlPreviewMainImageView: ImageView = itemView.findViewById(R.id.image_url_preview_main)
//        internal var padding: View = itemView.findViewById(R.id.view_group_chat_padding)

        internal fun bind(
            context: Context?,
            message: UserMessage,
            channel: GroupChannel?,
            isContinuous: Boolean,
            isNewDay: Boolean,
            isTempMessage: Boolean,
            isFailedMessage: Boolean,
            clickListener: OnItemClickListener?,
            longClickListener: OnItemLongClickListener?,
            position: Int
        ) {

            tvBody.text = message.message

            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = HelperData.formatDate(message.createdAt)
            } else {
                tvDate.visibility = View.GONE
            }

            tvTime.text = HelperData.formatTime(message.createdAt)

//            if (message.updatedAt > 0) {
//                editedText.visibility = View.VISIBLE
//            } else {
//                editedText.visibility = View.GONE
//            }
//
            when {
                isFailedMessage -> {
//                    readReceiptText.setText(R.string.message_failed)

                    ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_failed))
                    ivState.visibility = View.VISIBLE
                }
                isTempMessage -> {
//                    readReceiptText.setText(R.string.message_sending)

                    ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_try))
                    ivState.visibility = View.VISIBLE
                }
                else ->
                    // Since setChannel is set slightly after adapter is created
                    if (channel != null) {
                        val readReceipt = channel.getReadReceipt(message)
//                        if (readReceipt > 0) {
//                            readReceiptText.visibility = View.VISIBLE
//                            readReceiptText.text = readReceipt.toString()
//                        } else {
//                            readReceiptText.visibility = View.INVISIBLE
//                        }

                        ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_sent))
                        ivState.visibility = View.VISIBLE
                    }
            }

//            // If continuous from previous message, remove extra padding.
//            if (isContinuous) {
//                padding.visibility = View.GONE
//            } else {
//                padding.visibility = View.VISIBLE
//            }

            // If the message is sent on a different date than the previous one, display the date.


//            urlPreviewContainer.visibility = View.GONE
//            if (message.customType == URL_PREVIEW_CUSTOM_TYPE) {
//                try {
//                    urlPreviewContainer.visibility = View.VISIBLE
//                    val previewInfo = HelperUrl(message.data)
//                    urlPreviewSiteNameText.text = "@" + previewInfo.siteName
//                    urlPreviewTitleText.setText(previewInfo.title)
//                    urlPreviewDescriptionText.setText(previewInfo.description)
//                    HelperImage.displayImageFromUrl(mContext, previewInfo.imageUrl, urlPreviewMainImageView, null)
//                } catch (e: JSONException) {
//                    urlPreviewContainer.visibility = View.GONE
//                    e.printStackTrace()
//                }
//
//            }

            if (clickListener != null) {
                itemView.setOnClickListener { clickListener.onUserMessageItemClick(message) }
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onUserMessageItemLongClick(message, position)
                    true
                }
            }
        }

    }

    private inner class OtherUserMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvBody: TextView = itemView.findViewById(R.id.tvBody)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
//        internal var messageText: TextView = itemView.findViewById(R.id.text_group_chat_message)
//        internal var editedText: TextView = itemView.findViewById(R.id.text_group_chat_edited)
//        internal var nicknameText: TextView = itemView.findViewById(R.id.text_group_chat_nickname)
//        internal var timeText: TextView = itemView.findViewById(R.id.text_group_chat_time)
//        internal var readReceiptText: TextView = itemView.findViewById(R.id.text_group_chat_read_receipt)
//        internal var dateText: TextView = itemView.findViewById(R.id.text_group_chat_date)
//        internal var profileImage: ImageView = itemView.findViewById(R.id.image_group_chat_profile)
//
//        internal var urlPreviewContainer: ViewGroup = itemView.findViewById(R.id.url_preview_container)
//        internal var urlPreviewSiteNameText: TextView = itemView.findViewById(R.id.text_url_preview_site_name)
//        internal var urlPreviewTitleText: TextView = itemView.findViewById(R.id.text_url_preview_title)
//        internal var urlPreviewDescriptionText: TextView = itemView.findViewById(R.id.text_url_preview_description)
//        internal var urlPreviewMainImageView: ImageView = itemView.findViewById(R.id.image_url_preview_main)


        internal fun bind(context: Context?, message: UserMessage, channel: GroupChannel?, isNewDay: Boolean,
            isContinuous: Boolean, clickListener: OnItemClickListener?, longClickListener: OnItemLongClickListener?, position: Int) {

            tvBody.text = message.message

            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = HelperData.formatDate(message.createdAt)
            } else {
                tvDate.visibility = View.GONE
            }

            tvTime.text = HelperData.formatTime(message.createdAt)

//            if (message.updatedAt > 0) {
//                editedText.visibility = View.VISIBLE
//            } else {
//                editedText.visibility = View.GONE
//            }
//
//            when {
//                isFailedMessage -> {
//                    readReceiptText.setText(R.string.message_failed)
//                    readReceiptText.visibility = View.VISIBLE
//                }
//                isTempMessage -> {
//                    readReceiptText.setText(R.string.message_sending)
//                    readReceiptText.visibility = View.VISIBLE
//                }
//                else -> // Since setChannel is set slightly after adapter is created
//                    if (channel != null) {
//                        val readReceipt = channel.getReadReceipt(message)
//                        if (readReceipt > 0) {
//                            readReceiptText.visibility = View.VISIBLE
//                            readReceiptText.text = readReceipt.toString()
//                        } else {
//                            readReceiptText.visibility = View.INVISIBLE
//                        }
//                    }
//            }

//            // If continuous from previous message, remove extra padding.
//            if (isContinuous) {
//                padding.visibility = View.GONE
//            } else {
//                padding.visibility = View.VISIBLE
//            }

            // If the message is sent on a different date than the previous one, display the date.


//            urlPreviewContainer.visibility = View.GONE
//            if (message.customType == URL_PREVIEW_CUSTOM_TYPE) {
//                try {
//                    urlPreviewContainer.visibility = View.VISIBLE
//                    val previewInfo = HelperUrl(message.data)
//                    urlPreviewSiteNameText.text = "@" + previewInfo.siteName
//                    urlPreviewTitleText.setText(previewInfo.title)
//                    urlPreviewDescriptionText.setText(previewInfo.description)
//                    HelperImage.displayImageFromUrl(mContext, previewInfo.imageUrl, urlPreviewMainImageView, null)
//                } catch (e: JSONException) {
//                    urlPreviewContainer.visibility = View.GONE
//                    e.printStackTrace()
//                }
//
//            }

            if (clickListener != null) {
                itemView.setOnClickListener { clickListener.onUserMessageItemClick(message) }
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.onUserMessageItemLongClick(message, position)
                    true
                }
            }


//            // Since setChannel is set slightly after adapter is created
//            if (channel != null) {
//                val readReceipt = channel.getReadReceipt(message)
//                if (readReceipt > 0) {
//                    readReceiptText.visibility = View.VISIBLE
//                    readReceiptText.text = readReceipt.toString()
//                } else {
//                    readReceiptText.visibility = View.INVISIBLE
//                }
//            }
//
//            // Show the date if the message was sent on a different date than the previous message.
//            if (isNewDay) {
//                dateText.visibility = View.VISIBLE
//                dateText.text = HelperData.formatDate(message.createdAt)
//            } else {
//                dateText.visibility = View.GONE
//            }
//
//            // Hide profile image and nickname if the previous message was also sent by current sender.
//            if (isContinuous) {
//                profileImage.visibility = View.INVISIBLE
//                nicknameText.visibility = View.GONE
//            } else {
//                profileImage.visibility = View.VISIBLE
//                HelperImage.displayRoundImageFromUrl(mContext, message.sender.profileUrl, profileImage)
//
//                nicknameText.visibility = View.VISIBLE
//                nicknameText.text = message.sender.nickname
//            }
//
//            messageText.text = message.message
//            timeText.text = HelperData.formatTime(message.createdAt)
//
//            if (message.updatedAt > 0) {
//                editedText.visibility = View.VISIBLE
//            } else {
//                editedText.visibility = View.GONE
//            }
//
//            urlPreviewContainer.visibility = View.GONE
//            if (message.customType == URL_PREVIEW_CUSTOM_TYPE) {
//                try {
//                    urlPreviewContainer.visibility = View.VISIBLE
//                    val previewInfo = HelperUrl(message.data)
//                    urlPreviewSiteNameText.text = "@" + previewInfo.siteName
//                    urlPreviewTitleText.text = previewInfo.title
//                    urlPreviewDescriptionText.text = previewInfo.description
//                    HelperImage.displayImageFromUrl(mContext, previewInfo.imageUrl, urlPreviewMainImageView, null)
//                } catch (e: JSONException) {
//                    urlPreviewContainer.visibility = View.GONE
//                    e.printStackTrace()
//                }
//
//            }
//
//
//            if (clickListener != null) itemView.setOnClickListener { clickListener.onUserMessageItemClick(message) }
//
//            if (longClickListener != null) {
//                itemView.setOnLongClickListener {
//                    longClickListener.onUserMessageItemLongClick(message, position)
//                    true
//                }
//            }
        }
    }


    /**OTHER FILE VIEW HOLDER**/
    private inner class MyFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)

        internal fun bind(context: Context?, message: FileMessage, channel: GroupChannel?,
            isNewDay: Boolean, isTempMessage: Boolean, isFailedMessage: Boolean,
            tempFileMessageUri: Uri?, listener: OnItemClickListener?) {

            tvTitle.text = message.name
            tvTime.text = HelperData.formatTime(message.createdAt)

            when {
                isFailedMessage -> {
                    ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_failed))
                    ivState.visibility = View.VISIBLE
                }
                isTempMessage -> {
                    ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_try))
                    ivState.visibility = View.VISIBLE
                }
                else ->
                    // Since setChannel is set slightly after adapter is created
                    if (channel != null) {
                        ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_sent))
                        ivState.visibility = View.VISIBLE
                    }
            }

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = HelperData.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message) }

        }
    }

    private inner class OtherFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var tvBody: TextView = itemView.findViewById(R.id.tvBody)
        internal var fileSizeText: TextView? = null
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)

        internal fun bind(context: Context?, message: FileMessage, channel: GroupChannel?,
            isNewDay: Boolean, isContinuous: Boolean, listener: OnItemClickListener?) {

            tvTitle.text = message.name
            tvTime.text = HelperData.formatTime(message.createdAt)

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = HelperData.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message) }
        }
    }



    /**IMAGES FILE VIEW HOLDER**/
    private inner class MyImageFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivBody: ImageView = itemView.findViewById(R.id.ivBody)

        internal fun bind(context: Context, message: FileMessage, channel: GroupChannel?,
                          isNewDay: Boolean, isTempMessage: Boolean, isFailedMessage: Boolean,
                          tempFileMessageUri: Uri?, listener: OnItemClickListener?) {

            tvTime.text = HelperData.formatTime(message.createdAt)

            when {
                isFailedMessage -> {
                    ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_failed))
                    ivState.visibility = View.VISIBLE
                }
                isTempMessage -> {
                    ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_try))
                    ivState.visibility = View.VISIBLE
                }
                else ->
                    // Since setChannel is set slightly after adapter is created
                    if (channel != null) {
                        ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_sent))
                        ivState.visibility = View.VISIBLE
                    }
            }

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = HelperData.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE


            if (isTempMessage && tempFileMessageUri != null) {
                HelperImage.displayImageFromUrl(mContext, tempFileMessageUri.toString(), ivBody, null)
            } else {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<FileMessage.Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    if (message.type.toLowerCase().contains("gif")) {
                        HelperImage.displayGifImageFromUrl(
                            mContext,
                            message.url,
                            ivBody,
                            thumbnails[0].url,
                            ivBody.drawable
                        )
                    } else {
                        HelperImage.displayImageFromUrl(mContext, thumbnails[0].url, ivBody, ivBody.drawable)
                    }
                } else {
                    if (message.type.toLowerCase().contains("gif")) {
                        HelperImage.displayGifImageFromUrl(
                            mContext,
                            message.url,
                            ivBody,
                            null as String?,
                            ivBody.drawable
                        )
                    } else {
                        HelperImage.displayImageFromUrl(mContext, message.url, ivBody, ivBody.drawable)
                    }
                }
            }

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message) }

        }
    }

    private inner class OtherImageFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivBody: ImageView = itemView.findViewById(R.id.ivBody)

        internal fun bind(context: Context, message: FileMessage, channel: GroupChannel?,
                          isNewDay: Boolean, isContinuous: Boolean, listener: OnItemClickListener?) {

            tvTime.text = HelperData.formatTime(message.createdAt)

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = HelperData.formatDate(message.createdAt)
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
                    HelperImage.displayGifImageFromUrl(mContext, message.url, ivBody, thumbnails[0].url, ivBody.drawable)
                } else {
                    HelperImage.displayImageFromUrl(mContext, thumbnails[0].url, ivBody, ivBody.drawable)
                }
            } else {
                if (message.type.toLowerCase().contains("gif")) {
                    HelperImage.displayGifImageFromUrl(mContext, message.url, ivBody, null as String?, ivBody.drawable)
                } else {
                    HelperImage.displayImageFromUrl(mContext, message.url, ivBody, ivBody.drawable)
                }
            }

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message) }
        }
    }



    /**VIDEOS FILE VIEW HOLDER**/
    private inner class MyVideoFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var ivState: ImageView = itemView.findViewById(R.id.ivState)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivBody: ImageView = itemView.findViewById(R.id.ivBody)

        internal fun bind(context: Context?, message: FileMessage,
            channel: GroupChannel?, isNewDay: Boolean, isTempMessage: Boolean,
            isFailedMessage: Boolean, tempFileMessageUri: Uri?, listener: OnItemClickListener?) {

            tvTime.text = HelperData.formatTime(message.createdAt)
            when {
                isFailedMessage -> {
                    ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_failed))
                    ivState.visibility = View.VISIBLE
                }
                isTempMessage -> {
                    ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_try))
                    ivState.visibility = View.VISIBLE
                }
                else ->
                    // Since setChannel is set slightly after adapter is created
                    if (channel != null) {
                        ivState.setImageDrawable(mContext.getDrawable(R.drawable.ic_send_sent))
                        ivState.visibility = View.VISIBLE
                    }
            }

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = HelperData.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE


            if (isTempMessage && tempFileMessageUri != null) HelperImage.displayImageFromUrl(mContext, tempFileMessageUri.toString(), ivBody, null)
            else {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<FileMessage.Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    HelperImage.displayImageFromUrl(
                        mContext,
                        thumbnails[0].url,
                        ivBody,
                        ivBody.drawable
                    )
                }
            }

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message) }

        }
    }

    private inner class OtherVideoFileMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var tvTime: TextView = itemView.findViewById(R.id.tvTime)
        internal var tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        internal var tvDate: TextView = itemView.findViewById(R.id.tvDate)
        internal var ivBody: ImageView = itemView.findViewById(R.id.ivBody)

        internal fun bind(context: Context?, message: FileMessage, channel: GroupChannel?,
            isNewDay: Boolean, isContinuous: Boolean, listener: OnItemClickListener?) {

            tvTime.text = HelperData.formatTime(message.createdAt)

            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                tvDate.visibility = View.VISIBLE
                tvDate.text = HelperData.formatDate(message.createdAt)
            } else tvDate.visibility = View.GONE

            if (!isSingle) {
                // Hide profile image and nickname if the previous message was also sent by current sender.
                if (isContinuous) tvTitle.visibility = View.GONE
                else {
                    HelperImage.displayRoundImageFromUrl(mContext, message.sender.profileUrl, ivBody)

                    tvTitle.visibility = View.VISIBLE
                    tvTitle.text = message.sender.nickname
                }
            }else tvTitle.visibility = View.GONE

            // Get thumbnails from FileMessage
            val thumbnails = message.thumbnails as ArrayList<FileMessage.Thumbnail>

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size > 0) HelperImage.displayImageFromUrl(mContext, thumbnails[0].url, ivBody, ivBody.drawable)

            if (listener != null) itemView.setOnClickListener { listener.onFileMessageItemClick(message) }

        }
    }
}

