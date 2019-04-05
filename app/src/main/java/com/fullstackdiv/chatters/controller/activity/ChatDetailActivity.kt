package com.fullstackdiv.chatters.controller.activity

import android.Manifest
import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.helper.HelperUrl
import com.fullstackdiv.chatters.helper.HelperView
import com.fullstackdiv.chatters.helper.UserDefault
import com.fullstackdiv.chatters.helper.utils.FileUtils
import com.fullstackdiv.chatters.helper.utils.ImageUtils
import com.fullstackdiv.chatters.helper.utils.TextUtils
import com.google.android.material.snackbar.Snackbar
import com.sendbird.android.*
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.activity_chat_detail.*
import kotlinx.android.synthetic.main.in_menu_attachment.*
import org.json.JSONException
import java.io.File


class ChatDetailActivity : AppCompatActivity() {
    private val STATE_NORMAL = 0
    private val STATE_EDIT = 1
    private val STATE_CHANNEL_URL = "STATE_CHANNEL_URL"
    private val PERMISSION_WRITE_EXTERNAL_STORAGE = 13
    private val INTENT_REQUEST_CHOOSE_MEDIA = 301
    private val mFileProgressHandlerMap: HashMap<BaseChannel.SendFileMessageWithProgressHandler, FileMessage>? = null
    var mCurrentState = STATE_NORMAL

    val CONNECTION_HANDLER_ID = "CONNECTION_HANDLER_GROUP_CHAT"

    var CHANNEL_LIST_LIMIT = 30
    lateinit var channel_url :String
    var channel: GroupChannel? = null

    lateinit var adapterChat: ChatDetailAdapter

    lateinit var userDefault: UserDefault
    var isTyping = false

    var unreadCount = 0
    lateinit var layoutManager:LinearLayoutManager

    var state_compose = false

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Set this as true to restore background connection management.
        SendBird.setAutoBackgroundDetection(true)

        if (requestCode == INTENT_REQUEST_CHOOSE_MEDIA && resultCode == Activity.RESULT_OK) {
            // If user has successfully chosen the image, show a dialog to confirm upload.
            if (data == null) {
                Log.wtf("LOG_TAG", "data is null!")
                return
            }

            sendFileWithThumbnail(data.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_detail)
        userDefault = UserDefault.getInstance(applicationContext)
        postponeEnterTransition()

        val extras = intent.extras
        if (extras != null){
            channel_url = extras.getString("url", "")
            adapterChat = ChatDetailAdapter(this)
            refreshChannel()
        }else{
            HelperView.sLongToast(this, "Chat Invalid")
            finish()
        }
    }

    // Get Channel Detail
    private fun refreshChannel() {
        println("XXXASDF Refresh Started")
        if (channel == null) {
            GroupChannel.getChannel(channel_url, GroupChannel.GroupChannelGetHandler { groupChannel, e ->
                if (e != null) {
                    // Error!
                    e.printStackTrace()
                    return@GroupChannelGetHandler
                }

                channel = groupChannel
                adapterChat.setChannel(channel!!)

                // Get Channel Message
                adapterChat.loadLatestMessages(CHANNEL_LIST_LIMIT,
                    BaseChannel.GetMessagesHandler { _, _ ->
                        adapterChat.markAllMessagesAsRead() })

                setChannelHandler()
            })
        } else {
            channel?.refresh(GroupChannel.GroupChannelRefreshHandler { e ->
                if (e != null) {
                    // Error!
                    e.printStackTrace()
                    return@GroupChannelRefreshHandler
                }

                // Get Channel Message
                adapterChat.loadLatestMessages(CHANNEL_LIST_LIMIT,
                    BaseChannel.GetMessagesHandler { _, _ ->
                        adapterChat.markAllMessagesAsRead() })

                setChannelHandler()
            })
        }
    }

    // Set Channel Handler
    fun setChannelHandler(){
        SendBird.addChannelHandler(channel_url, object : SendBird.ChannelHandler() {
            override fun onMessageReceived(baseChannel: BaseChannel, baseMessage: BaseMessage) {
                if (baseChannel.url == channel_url) {
                    adapterChat.markAllMessagesAsRead()

                    // Add new message to view
                    adapterChat.addMessage(baseMessage)

                    if(layoutManager.findFirstVisibleItemPosition() != 0) {
                        unreadCount+= 1
                        tvBellowCount.text = unreadCount.toString()
                        tvBellowCount.visibility = View.VISIBLE
                    }
                }
            }

            override fun onMessageDeleted(baseChannel: BaseChannel?, msgId: Long) {
                super.onMessageDeleted(baseChannel, msgId)
                if (baseChannel!!.url == channel_url) adapterChat.delete(msgId)
            }

            override fun onMessageUpdated(channel: BaseChannel?, message: BaseMessage?) {
                super.onMessageUpdated(channel, message)
                if (channel!!.url == channel_url) adapterChat.update(message!!)
            }

            override fun onReadReceiptUpdated(channel: GroupChannel?) {
                if (channel!!.url == channel_url) adapterChat.notifyDataSetChanged()
            }

            override fun onTypingStatusUpdated(channel: GroupChannel?) {
                if (channel!!.url == channel_url) displayTyping(channel.typingMembers)
            }
        })

        setRecyclerView()
    }

    private fun setRecyclerView() {
        layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.isSmoothScrollbarEnabled = true

        rv.layoutManager = layoutManager
        rv.itemAnimator = DefaultItemAnimator()
        rv.adapter = adapterChat

        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView : RecyclerView, newState: Int) {
                if (layoutManager.findLastVisibleItemPosition() == adapterChat.itemCount - 1) {
                    adapterChat.loadPreviousMessages(CHANNEL_LIST_LIMIT, null)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0) {
                    //check for scroll up
                    if (layoutManager.findFirstVisibleItemPosition() > 0) fabBellow.show()

                }else if (dy > 0){
                    //check for scroll down
                    if (layoutManager.findFirstVisibleItemPosition() == 0) {
                        unreadCount = 0
                        tvBellowCount.visibility = View.GONE
                        fabBellow.hide()
                    }
                }
            }
        })

        setBaseView()
    }

    fun setBaseView(){
        tvTitle.text = if (channel != null)  TextUtils.getGroupChannelTitle(channel!!) else ""
        Picasso.with(this)
            .load(ImageUtils.getGroupChannelImage(channel!!))
            .resize(100,100)
            .transform(CropCircleTransformation())
            .into(ivPP)
        setAction()
    }


    fun setAction(){
        ivBack.setOnClickListener { onBackPressed()  }

        fabSend.setOnClickListener {
            if (state_compose) sendMsg(etChat.text.toString())
            else toggleMenu()
        }

        btCloseMenu.setOnClickListener { toggleMenu() }

        fabBellow.setOnClickListener {
            rv.smoothScrollToPosition(0)
        }

        etChat.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable) {
                state_compose = if (p0.isNotEmpty()) {
                    channel?.startTyping()
                    fabSend.setImageDrawable(getDrawable(R.drawable.ic_send_white))
                    true
                } else {
                    channel?.endTyping()
                    fabSend.setImageDrawable(getDrawable(R.drawable.ic_attachment_white))
                    false
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        etChat.imeOptions = EditorInfo.IME_ACTION_SEND
        etChat.setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        etChat.setOnEditorActionListener {
                _, i, _ ->
            if (i == EditorInfo.IME_ACTION_SEND) {
                if (!etChat.text.isNullOrBlank()) {
                    sendMsg(etChat.text.toString())
                }
            }
            true
        }

        etChat.setOnClickListener { if (CLAttachMenu.isVisible) toggleMenu() }

        etChat.onFocusChangeListener = View.OnFocusChangeListener { p0, _ ->
            if (p0!!.hasFocus() && CLAttachMenu.isVisible) toggleMenu()
        }

        adapterChat.setItemClickListener(object : ChatDetailAdapter.OnItemClickListener {
            override fun onUserMessageItemClick(message: UserMessage) {
                // Restore failed message and remove the failed message from list.
                if (adapterChat.isFailedMessage(message)) {
                    retryFailedMessage(message)
                    return
                }

                // Message is sending. Do nothing on click event.
                if (adapterChat.isTempMessage(message)) return



                if (message.customType == adapterChat.URL_PREVIEW_CUSTOM_TYPE) {
                    try {
                        val info = HelperUrl(message.data)
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.url))
                        startActivity(browserIntent)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
            }

            override fun onFileMessageItemClick(message: FileMessage) {
                // Load media chooser and remove the failed message from list.
                if (adapterChat.isFailedMessage(message)) {
                    retryFailedMessage(message)
                    return
                }

                // Message is sending. Do nothing on click event.
                if (adapterChat.isTempMessage(message)) return



                onFileMessageClicked(message)
            }
        })

        adapterChat.setItemLongClickListener(object : ChatDetailAdapter.OnItemLongClickListener {
            override fun onUserMessageItemLongClick(message: UserMessage, position: Int) {
                showMessageOptionsDialog(message, position)
            }

            override fun onFileMessageItemLongClick(message: FileMessage) {}
            override fun onAdminMessageItemLongClick(message: AdminMessage) {}
        })

        // Attachment Menu button
        btDoc.setOnClickListener {  requestMedia() }
    }



    /*FUN*/
    private fun displayTyping(typingUsers: List<Member>) {
        if (typingUsers.isNotEmpty()) {
            tvSub.visibility = View.VISIBLE

            tvSub.text = when {
                channel?.members?.size == 2 -> "Typing . . ."
                typingUsers.size == 1 -> typingUsers[0].nickname + " is typing"
                typingUsers.size == 2 -> typingUsers[0].nickname + " " + typingUsers[1].nickname + " is typing"
                else -> "Multiple users are typing"
            }
        } else tvSub.visibility = View.INVISIBLE
    }

    fun stateTyping(typing: Boolean){
        if (channel == null) return

        if (typing) {
            isTyping = true
            channel?.startTyping()
        } else {
            isTyping = false
            channel?.endTyping()
        }
    }

    fun sendMsg(msg:String){
        channel?.sendUserMessage(msg) { userMessage, e ->
            if (e != null) {
                e.printStackTrace()
                adapterChat.markMessageFailed(userMessage.requestId)
                return@sendUserMessage
            }else{
                rv.scrollToPosition(0)
                adapterChat.addMessage(userMessage)
                adapterChat.markMessageSent(userMessage)
                etChat.setText("")
            }
        }
    }

    fun toggleMenu() {
        val x = CLAttachMenu.measuredWidth - fabSend.width/2
        val y = CLAttachMenu.measuredHeight
        val endRadius = Math.hypot(CLAttachMenu.width.toDouble(), CLAttachMenu.height.toDouble())
        val startRadius = Math.hypot(CLAttachMenu.width.toDouble(), CLAttachMenu.height.toDouble())

        if (!CLAttachMenu.isVisible) {
            // Reveal Animation
            val anim = ViewAnimationUtils.createCircularReveal(CLAttachMenu, x, y, 0f, endRadius.toFloat())

            CLAttachMenu!!.visibility = View.VISIBLE
            btCloseMenu.visibility = View.VISIBLE
            CLAttachMenu.requestFocus()
            anim.start()
        } else {
            // Hide Animation
            val anim = ViewAnimationUtils.createCircularReveal(CLAttachMenu, x, y, startRadius.toFloat(), 0f)

            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {}
                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}

                override fun onAnimationEnd(animator: Animator) {
                    if (CLAttachMenu != null) CLAttachMenu!!.visibility = View.INVISIBLE
                    if (btCloseMenu != null) btCloseMenu!!.visibility = View.GONE
                }
            })
            anim.start()
        }
    }


    /*MJDSHIFHIDYHOALJDMOAIDYOIDMJCHFYAUFEYCUFVTYDFNUYTNGFU^RBFYU*/
    private fun requestMedia() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // If storage permissions are not granted, request permissions at run-time,
            // as per < API 23 guidelines.
            requestStoragePermissions()
        } else {
            val intent = Intent()

            // Pick images or videos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent.type = "*/*"
                val mimeTypes = arrayOf("image/*", "video/*")
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            } else {
                intent.type = "image/* video/*"
            }

            intent.action = Intent.ACTION_GET_CONTENT

            // Always show the chooser (if there are multiple options available)
            startActivityForResult(Intent.createChooser(intent, "Select Media"), INTENT_REQUEST_CHOOSE_MEDIA)

            // Set this as false to maintain connection
            // even when an external Activity is started.
            SendBird.setAutoBackgroundDetection(false)
        }
    }

    private fun requestStoragePermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Snackbar.make(CLMainRoot, "Storage access permissions are required to upload/download files.",
                Snackbar.LENGTH_LONG)
                .setAction("Okay") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            PERMISSION_WRITE_EXTERNAL_STORAGE
                        )
                    }
                }
                .show()
        } else {
            // Permission has not been granted yet. Request it directly.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    private fun onFileMessageClicked(message: FileMessage) {
        val type = message.type.toLowerCase()
//        if (type.startsWith("image")) {
//            val i = Intent(this, PhotoViewerActivity::class.java)
//            i.putExtra("url", message.url)
//            i.putExtra("type", message.type)
//            startActivity(i)
//        } else if (type.startsWith("video")) {
//            val intent = Intent(this, MediaPlayerActivity::class.java)
//            intent.putExtra("url", message.url)
//            startActivity(intent)
//        } else {
//            showDownloadConfirmDialog(message)
//        }
    }

    private fun sendFileWithThumbnail(uri: Uri) {
        if (channel == null) return


        // Specify two dimensions of thumbnails to generate
        val thumbnailSizes = arrayListOf<FileMessage.ThumbnailSize>()
        thumbnailSizes.add(FileMessage.ThumbnailSize(240, 240))
        thumbnailSizes.add(FileMessage.ThumbnailSize(320, 320))

        val info = FileUtils.getFileInfo(this, uri)

        if (info == null) {
            Toast.makeText(this, "Extracting file information failed.", Toast.LENGTH_LONG).show()
            return
        }

        val path = info["path"] as String
        val file = File(path)
        val name = file.name
        val mime = info["mime"] as String
        val size = info["size"] as Int

        if (path == "") {
            Toast.makeText(this, "File must be located in local storage.", Toast.LENGTH_LONG).show()
        } else {
            val progressHandler = object : BaseChannel.SendFileMessageWithProgressHandler {
                override fun onProgress(bytesSent: Int, totalBytesSent: Int, totalBytesToSend: Int) {
                    val fileMessage = mFileProgressHandlerMap?.get(this)
                    if (fileMessage != null && totalBytesToSend > 0) {
                        val percent = totalBytesSent * 100 / totalBytesToSend
                        adapterChat.setFileProgressPercent(fileMessage, percent)
                    }
                }

                override fun onSent(fileMessage: FileMessage, e: SendBirdException?) {
                    if (e != null) {
                        HelperView.sLongToast(this@ChatDetailActivity, "" + e.code + ":" + e.message)
                        adapterChat.markMessageFailed(fileMessage.requestId)
                        return
                    }

                    adapterChat.markMessageSent(fileMessage)
                }
            }

            // Send image with thumbnails in the specified dimensions
            val tempFileMessage =
                channel?.sendFileMessage(file, name, mime, size, "", null, thumbnailSizes, progressHandler)

            mFileProgressHandlerMap?.put(progressHandler, tempFileMessage!!)

            adapterChat.addTempFileMessageInfo(tempFileMessage!!, uri)
            adapterChat.addMessage(tempFileMessage)
        }
    }

    private fun showMessageOptionsDialog(message: BaseMessage, position: Int) {
        val options = arrayOf("Edit message", "Delete message")

//        val builder = AlertDialog.Builder(this)
//        builder.setItems(options, DialogInterface.OnClickListener { dialog, which ->
//            if (which == 0) {
//                setState(STATE_EDIT, message, position)
//            } else if (which == 1) {
//                deleteMessage(message)
//            }
//        })
//        builder.create().show()
    }

    private fun setState(state: Int, editingMessage: BaseMessage, position: Int) {
//        when (state) {
//            STATE_NORMAL -> {
//                mCurrentState = STATE_NORMAL
//                mEditingMessage = null
//
//                mUploadFileButton.setVisibility(View.VISIBLE)
//                mMessageSendButton.setText("SEND")
//                mMessageEditText.setText("")
//            }
//
//            STATE_EDIT -> {
//                mCurrentState = STATE_EDIT
//                mEditingMessage = editingMessage
//
//                mUploadFileButton.setVisibility(View.GONE)
//                mMessageSendButton.setText("SAVE")
//                var messageString: String? = (editingMessage as UserMessage).message
//                if (messageString == null) {
//                    messageString = ""
//                }
//                mMessageEditText.setText(messageString)
//                if (messageString.length > 0) {
//                    mMessageEditText.setSelection(0, messageString.length)
//                }
//
//                mMessageEditText.requestFocus()
//                mMessageEditText.postDelayed(Runnable {
//                    mIMM.showSoftInput(mMessageEditText, 0)
//
//                    mRecyclerView.postDelayed(Runnable { mRecyclerView.scrollToPosition(position) }, 500)
//                }, 100)
//            }
//        }
    }

    private fun retryFailedMessage(message: BaseMessage) {
//        AlertDialog.Builder(this)
//            .setMessage("Retry?")
//            .setPositiveButton(R.string.resend_message, DialogInterface.OnClickListener { dialog, which ->
//                if (which == DialogInterface.BUTTON_POSITIVE) {
//                    if (message is UserMessage) {
//                        val userInput = message.message
//                        sendUserMessage(userInput)
//                    } else if (message is FileMessage) {
//                        val uri = adapterChat.getTempFileMessageUri(message)
//                        sendFileWithThumbnail(uri)
//                    }
//                    adapterChat.removeFailedMessage(message)
//                }
//            })
//            .setNegativeButton(R.string.delete_message, DialogInterface.OnClickListener { dialog, which ->
//                if (which == DialogInterface.BUTTON_NEGATIVE) {
//                    adapterChat.removeFailedMessage(message)
//                }
//            }).show()
    }

//
//    public override fun onResume() {
//        super.onResume()
//
//        adapterChat.setContext(this) // Glide bug fix (java.lang.IllegalArgumentException: You cannot start a load for a destroyed activity)
//        println("XXXASDF onResume")
//        refreshChannel()
//
////        ConnectionManager.addNetworkHandler(CONNECTION_HANDLER_ID,  object: ConnectionManager.NetworkHandler(){
////            override fun onReconnected() {
////                println("XXXASDF onResume refreshChannel")
////                refreshChannel()
////            }
////        })
//    }

    override fun onBackPressed() {
        if (CLAttachMenu!!.visibility == View.VISIBLE) toggleMenu()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateTyping(false)
        SendBird.removeChannelHandler(channel_url)
    }

    override fun onPause() {
        super.onPause()
        if (CLAttachMenu.isVisible) toggleMenu()
    }

}
