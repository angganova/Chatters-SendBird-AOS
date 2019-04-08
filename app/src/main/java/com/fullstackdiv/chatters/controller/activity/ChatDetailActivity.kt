package com.fullstackdiv.chatters.controller.activity

import android.Manifest
import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.controller.activity.adapter.ChatDetailAdapter
import com.fullstackdiv.chatters.controller.extension.FloatingView
import com.fullstackdiv.chatters.helper.utils.PopUpUtils
import com.fullstackdiv.chatters.helper.UserDefault
import com.fullstackdiv.chatters.helper.utils.*
import com.google.android.material.snackbar.Snackbar
import com.sendbird.android.*
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.activity_chat_detail.*
import kotlinx.android.synthetic.main.in_menu_attachment.*
import org.json.JSONException
import java.io.File


class ChatDetailActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    private val STATE_NORMAL = 0
    private val STATE_EDIT = 1
    private val STATE_CHANNEL_URL = "STATE_CHANNEL_URL"
    private val PERMISSION_WRITE_EXTERNAL_STORAGE = 13
    private val INTENT_REQUEST_CHOOSE_MEDIA = 301

    private val INTENT_REQ_DOCUMENT = 901
    private val INTENT_REQ_CAMERA = 902
    private val INTENT_REQ_GALLERY = 903
    private val INTENT_REQ_AUDIO = 904
    private val INTENT_REQ_LOCATION = 905
    private val INTENT_REQ_CONTACT = 906

    private val mFileProgressHandlerMap:
            HashMap<BaseChannel.SendFileMessageWithProgressHandler, FileMessage>? = null
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
    var opt_menu = 0

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Set this as true to restore background connection management.
        SendBird.setAutoBackgroundDetection(true)

        if (resultCode == Activity.RESULT_OK) {
            // If user has successfully chosen the image, show a dialog to confirm upload.

            when(requestCode){
                INTENT_REQ_GALLERY->{

                }
            }

            if (data != null && data.data != null) {
                println("XXXASDF DATA : $data")
                println("XXXASDF DATA url : ${data.data}")
                println("XXXASDF DATA categories : ${data.categories}")
                println("XXXASDF DATA type : ${data.type}")
//                sendFileWithThumbnail(data.data!!)
            }else{
                Log.wtf("LOG_TAG", "data is null!")
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_detail)
        setSupportActionBar(toolbar)

        userDefault = UserDefault.getInstance(applicationContext)
        postponeEnterTransition()

        val extras = intent.extras
        if (extras != null){
            channel_url = extras.getString("url", "")
            adapterChat = ChatDetailAdapter(this)
            refreshChannel()
        }else{
            PopUpUtils.sLongToast(this, "Chat Invalid")
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
    private fun setChannelHandler(){
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

    // Set Chat List
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

    // Set Base View Display
    private fun setBaseView(){
        tvTitle.text = if (channel != null)  TextUtils.getGroupChannelTitle(channel!!) else ""

        Picasso.with(this)
            .load(ImageUtils.getGroupChannelImage(channel!!))
            .resize(100,100)
            .transform(CropCircleTransformation())
            .into(ivPP)
        setBaseAction()

        toolbar.setNavigationIcon(R.drawable.ic_back_white)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    // Set Base Action
    private fun setBaseAction(){
        fabSend.setOnClickListener {
            if (state_compose) sendMsg(etChat.text.toString())
            else toggleAttachMenu()
//            else openDialog()
//            requestMedia()
        }

        ivChBg.setOnClickListener { changeBackground() }

        btCloseMenu.setOnClickListener { toggleAttachMenu() }

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

        etChat.setOnClickListener { if (CLAttachMenu.isVisible) toggleAttachMenu() }

        etChat.onFocusChangeListener = View.OnFocusChangeListener { p0, _ ->
            if (p0!!.hasFocus() && CLAttachMenu.isVisible) toggleAttachMenu()
        }

        adapterChat.setItemClickListener(object : ChatDetailAdapter.OnItemClickListener {
            override fun onUserMessageItemClick(message: UserMessage, position: Int) {
                // Restore failed message and remove the failed message from list.
                if (adapterChat.isFailedMessage(message)) {
                    retryFailedMessage(message)
                    return
                }

                // Message is sending. Do nothing on click event.
                if (adapterChat.isTempMessage(message)) return

                if (adapterChat.selection_state){
                    if (adapterChat.isSelected(position)) {
                        unSelectMsg(position)
                        if (adapterChat.selectedCount() == 0) endSelection()
                    } else selectMsg(position)

                }

                if (message.customType == adapterChat.URL_PREVIEW_CUSTOM_TYPE) {
                    try {
                        val info = UrlUtils(message.data)
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.url))
                        startActivity(browserIntent)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
            }

            override fun onFileMessageItemClick(message: FileMessage, position: Int) {
                // Load media chooser and remove the failed message from list.
                if (adapterChat.isFailedMessage(message)) {
                    retryFailedMessage(message)
                    return
                }

                // Message is sending. Do nothing on click event.
                if (adapterChat.isTempMessage(message)) return


                if(adapterChat.selection_state){
                    if (adapterChat.isSelected(position)) {
                        unSelectMsg(position)
                        if (adapterChat.selectedCount() == 0) endSelection()
                    } else selectMsg(position)

                }else onFileMessageClicked(message)
            }
        })

        adapterChat.setItemLongClickListener(object : ChatDetailAdapter.OnItemLongClickListener {
            override fun onUserMessageItemLongClick(message: UserMessage, position: Int) {
                if (adapterChat.selection_state) {
                    if (adapterChat.isSelected(position)) {
                        unSelectMsg(position)
                        if (adapterChat.selectedCount() == 0) endSelection()
                    } else selectMsg(position)
                } else {
                    startSelection()
                    selectMsg(position)
                }
            }

            override fun onFileMessageItemLongClick(message: FileMessage, position: Int) {
                if (adapterChat.selection_state) {
                    if (adapterChat.isSelected(position)) {
                        unSelectMsg(position)
                        if (adapterChat.selectedCount() == 0) endSelection()
                    } else selectMsg(position)
                } else {
                    startSelection()
                    selectMsg(position)
                }
            }
            override fun onAdminMessageItemLongClick(message: AdminMessage, position: Int) {
                if (adapterChat.selection_state) {
                    if (adapterChat.isSelected(position)) {
                        unSelectMsg(position)
                        if (adapterChat.selectedCount() == 0) endSelection()
                    } else selectMsg(position)
                } else {
                    startSelection()
                    selectMsg(position)
                }
            }
        })


        val attachmentIntent = Intent(this, RequestMediaActivity::class.java)
        // Attachment Menu button
        btDoc.setOnClickListener { requestMedia() }
        btCam.setOnClickListener { requestMedia() }
        btGallery.setOnClickListener {
            attachmentIntent.putExtra("type", "Images")
            startActivity(attachmentIntent)
        }
        btAudio.setOnClickListener { requestMedia() }
        btLoc.setOnClickListener { requestMedia() }
        btContact.setOnClickListener {
            attachmentIntent.putExtra("type", "Contacts")
            startActivity(attachmentIntent)
        }
    }


    /**User Chat Action**/
    fun startSelection(){
        setSelectionActBar(View.OnClickListener {
                setNormalActBar()
                endSelection()
            })
        adapterChat.selection_state = true
    }

    fun endSelection(){
        setNormalActBar()
        adapterChat.clearSelection()
    }

    fun setSelectionActBar(click: View.OnClickListener){
        if (opt_menu == 1) return

        opt_menu = 1
        CLToolbar.visibility = View.GONE
        toolbar.setNavigationOnClickListener(click)
        invalidateOptionsMenu()
    }

    fun setNormalActBar(){
        if (opt_menu == 0) return

        opt_menu = 0
        CLToolbar.visibility = View.VISIBLE
        toolbar.setNavigationOnClickListener { onBackPressed() }
        invalidateOptionsMenu()
    }

    fun selectMsg(pos: Int){
        adapterChat.select(pos)
        updateToolbarMenuCounter(adapterChat.selectedCount())
    }

    fun unSelectMsg(pos: Int){
        adapterChat.unSelect(pos)
        updateToolbarMenuCounter(adapterChat.selectedCount())
    }

    fun updateToolbarMenuCounter(count:Int){
        toolbar.title = count.toString()
        invalidateOptionsMenu()
    }

    /**Other Function**/

    // Display Typing Indicator for other user
    private fun displayTyping(typingUsers: List<Member>) {
        if (typingUsers.isNotEmpty()) {
            tvSub.text = when {
                channel?.members?.size == 2 -> "Typing . . ."
                typingUsers.size == 1 -> typingUsers[0].nickname + " is typing"
                typingUsers.size == 2 -> typingUsers[0].nickname + " " + typingUsers[1].nickname + " is typing"
                else -> "Multiple users are typing"
            }
        } else tvSub.text = ""
    }

    // Set Typing state
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

    // Send Message in Channel
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

    private fun openDialog() {
//        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val inflater = LayoutInflater.from(this)
        // inflate the custom popup layout
        val view = inflater.inflate(R.layout.in_menu_attachment,
            null)

        val btDoc:ImageButton = view.findViewById(R.id.btDoc)
        btDoc.setOnClickListener { FloatingView.dismissWindow() }

        FloatingView.onShowPopup(this, view, etChat)
    }



    // Toggle Attachment Menu
    fun toggleAttachMenu() {
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

    private fun requestFile(code:Int){
        if (!checkPermission()) return

        val intent = Intent()

        // Pick images
        when(code){
            INTENT_REQ_DOCUMENT -> intent.type = "image/*"
            INTENT_REQ_CAMERA -> intent.type = "image/*"
            INTENT_REQ_GALLERY -> intent.type = "image/*"
            INTENT_REQ_AUDIO -> intent.type = "image/*"
            INTENT_REQ_LOCATION -> intent.type = "image/*"
            INTENT_REQ_CONTACT -> intent.type = "image/*"
        }
        intent.action = Intent.ACTION_GET_CONTENT

        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(
            intent, "Select ${
                when(code){
                    INTENT_REQ_DOCUMENT -> "Document"
                    INTENT_REQ_CAMERA -> "Camera"
                    INTENT_REQ_GALLERY -> "Image"
                    INTENT_REQ_AUDIO -> "Audio"
                    INTENT_REQ_LOCATION -> "Location"
                    INTENT_REQ_CONTACT -> "Contact"
                    else -> ""
                }
            }"),
            INTENT_REQUEST_CHOOSE_MEDIA)

        // Set this as false to maintain connection
        // even when an external Activity is started.
        SendBird.setAutoBackgroundDetection(false)

    }

    private fun checkPermission():Boolean{
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions()
            return false
        }
        return true
    }

    /*MJDSHIFHIDYHOALJDMOAIDYOIDMJCHFYAUFEYCUFVTYDFNUYTNGFU^RBFYU*/

    // Request Media Permission & get media
    private fun requestMedia() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
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
            } else intent.type = "image/* video/*"


            intent.action = Intent.ACTION_GET_CONTENT

            // Always show the chooser (if there are multiple options available)
            startActivityForResult(Intent.createChooser(
                intent, "Select Media"), INTENT_REQUEST_CHOOSE_MEDIA)

            // Set this as false to maintain connection
            // even when an external Activity is started.
            SendBird.setAutoBackgroundDetection(false)
        }
    }


    // Request Storage Permission
    private fun requestStoragePermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
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

    // Type File Message Clicked
    private fun onFileMessageClicked(message: FileMessage) {
        val type = message.type.toLowerCase()
        when {
            type.startsWith("image") -> {
                DialogUtils.sDialogImage(this, message.url)
            }
            type.startsWith("video") -> {
//                val intent = Intent(this, MediaPlayerActivity::class.java)
//                intent.putExtra("url", message.url)
//                startActivity(intent)
            }
            else -> showDownloadConfirmDialog(message)
        }
    }


    private fun showDownloadConfirmDialog(message: FileMessage) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If storage permissions are not granted, request permissions at run-time,
            // as per < API 23 guidelines.
            requestStoragePermissions()
        } else {
            DialogUtils.showDialog2(this, "Download file?", "Download",
                DialogInterface.OnClickListener { dialog, which ->
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        FileUtils.downloadFile(this, message.url, message.name)
                    }
                })
        }

    }

    private fun sendFileImage(uri: Uri){
        if (channel == null) return

        // Specify two dimensions of thumbnails to generate
        val thumbnailSizes = arrayListOf<FileMessage.ThumbnailSize>()
        thumbnailSizes.add(FileMessage.ThumbnailSize(240, 240))
        thumbnailSizes.add(FileMessage.ThumbnailSize(320, 320))

        val info = FileUtils2.getFileInfo(this, uri)

        if (info == null) {
            Toast.makeText(this,
                "Extracting file information failed.", Toast.LENGTH_LONG).show()
            return
        }

        val path = info["path"] as String
        val file = File(path)
        val name = file.name
        val mime = info["mime"] as String
        val size: Int = info["size"] as Int

        if (path == "") {
            Toast.makeText(this,
                "File must be located in local storage.", Toast.LENGTH_LONG).show()
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
                        PopUpUtils.sLongToast(this@ChatDetailActivity, "" + e.code + ":" + e.message)
                        adapterChat.markMessageFailed(fileMessage.requestId)
                        return
                    }

                    adapterChat.markMessageSent(fileMessage)
                }
            }

            // Send image with thumbnails in the specified dimensions
            val tempFileMessage =
                channel?.sendFileMessage(file, name, mime, size,
                    "", null, thumbnailSizes, progressHandler)

            mFileProgressHandlerMap?.put(progressHandler, tempFileMessage!!)

            adapterChat.addTempFileMessageInfo(tempFileMessage!!, uri)
            adapterChat.addMessage(tempFileMessage)
        }
    }

    private fun sendFileWithThumbnail(uri: Uri) {
        if (channel == null) return

        // Specify two dimensions of thumbnails to generate
        val thumbnailSizes = arrayListOf<FileMessage.ThumbnailSize>()
        thumbnailSizes.add(FileMessage.ThumbnailSize(240, 240))
        thumbnailSizes.add(FileMessage.ThumbnailSize(320, 320))

        val info = FileUtils.getFileInfo(this, uri)

        if (info == null) {
            Toast.makeText(this,
                "Extracting file information failed.", Toast.LENGTH_LONG).show()
            return
        }

        val path = info["path"] as String
        val file = File(path)
        val name = file.name
        val mime = info["mime"] as String
        val size: Int = info["size"] as Int

        if (path == "") {
            Toast.makeText(this,
                "File must be located in local storage.", Toast.LENGTH_LONG).show()
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
                        PopUpUtils.sLongToast(this@ChatDetailActivity, "" + e.code + ":" + e.message)
                        adapterChat.markMessageFailed(fileMessage.requestId)
                        return
                    }

                    adapterChat.markMessageSent(fileMessage)
                }
            }

            // Send image with thumbnails in the specified dimensions
            val tempFileMessage =
                channel?.sendFileMessage(file, name, mime, size,
                    "", null, thumbnailSizes, progressHandler)

            mFileProgressHandlerMap?.put(progressHandler, tempFileMessage!!)

            adapterChat.addTempFileMessageInfo(tempFileMessage!!, uri)
            adapterChat.addMessage(tempFileMessage)
        }
    }

    // Delete all Selected Chat
    private fun deleteSelectedChat() {
        compositeDisposable.add (
            Observable.fromIterable(adapterChat.getSelectedChat())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<BaseMessage>() {
                    override fun onComplete() {
                        // Re-query message list
                        endSelection()
                        PopUpUtils.sLongToast(this@ChatDetailActivity,
                            "Message Deleted")
                    }

                    override fun onNext(msg: BaseMessage) {
                        channel!!.deleteMessage(msg) {
                            if (it != null){
                                it.printStackTrace()
                                return@deleteMessage
                            }
                        }
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                })
        )
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

    fun changeBackground(){
        when(DataUtils.getRandomInt(1,6)){
            1 -> rv.background = ContextCompat.getDrawable(this, R.drawable.background1)
            2 -> rv.background = ContextCompat.getDrawable(this, R.drawable.background2)
            3 -> rv.background = ContextCompat.getDrawable(this, R.drawable.background3)
            4 -> rv.background = ContextCompat.getDrawable(this, R.drawable.background4)
            5 -> rv.background = ContextCompat.getDrawable(this, R.drawable.background5)
            6 -> rv.background = ContextCompat.getDrawable(this, R.color.white)
        }
    }


    /**OVERRIDE**/
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.channel_detail_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mnDelete ->{
                DialogUtils.showDialog2(this,
                    "Delete Chat ?", "Delete",
                    DialogInterface.OnClickListener{ dialog, _ ->
                        deleteSelectedChat()
                        dialog.dismiss()
                    })
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        when {
            adapterChat.selection_state -> {
                setNormalActBar()
                endSelection()
            }
            CLAttachMenu!!.visibility == View.VISIBLE -> toggleAttachMenu()
            else -> super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        if (CLAttachMenu.isVisible) toggleAttachMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateTyping(false)
        SendBird.removeChannelHandler(channel_url)
        compositeDisposable.clear()
    }
}
