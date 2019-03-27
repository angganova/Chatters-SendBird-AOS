package com.fullstackdiv.chatters.controller.fragment

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.controller.activity.MainActivity
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.PreviousMessageListQuery
import kotlinx.android.synthetic.main.fragment_chat_detail.*


/**
 * Created by Angga N P on 3/27/2019.
 */

class ChatDetailFragment: Fragment(){
    val TRX_ID = "channel"

    lateinit var ch_url:String
    var msgList: MutableList<BaseMessage>? = null
    lateinit var channel: GroupChannel
    lateinit var adapter: MessageAdapter

    fun newInstance(url:String): ChatDetailFragment {
        val args = Bundle()
        args.putString(TRX_ID, url)
        val fragment = ChatDetailFragment()
        fragment.arguments = args
        return fragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        if (arguments != null) ch_url = arguments!!.getString(TRX_ID, "")
        return inflater.inflate(R.layout.fragment_chat_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MainActivity).hideLO()

        if (ch_url.isNotBlank()) getCH()
    }

    fun setBaseView(){
        rv.layoutManager = LinearLayoutManager(this.context)
        rv.itemAnimator = DefaultItemAnimator()

        adapter = MessageAdapter(activity!!, msgList!!)
        rv.adapter = adapter

        setAct()
    }

    fun setAct(){
        fabSend.setOnClickListener {
            if (!etChat.text.isNullOrBlank()) {
                sendMsg(etChat.text.toString())
            }
        }

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
    }

    fun getCH(){
        GroupChannel.getChannel(ch_url, GroupChannel.GroupChannelGetHandler { groupChannel, e ->
            if (e != null) {
                // Error!
                return@GroupChannelGetHandler
            }else{
                channel = groupChannel
                getListMessage(0)
            }
        })
    }


    fun getListMessage(count:Int){
        val prevMessageListQuery = channel.createPreviousMessageListQuery()
        prevMessageListQuery.load(count, true, PreviousMessageListQuery.MessageListQueryResult { messages, e ->
            if (e != null) {
                // Error.
                return@MessageListQueryResult
            }else{
                if (msgList.isNullOrEmpty()) msgList = messages
                else {
                    msgList!!.addAll(messages)
                    adapter.addMessageList(messages)
                }

                setBaseView()
            }
        })
    }


    fun sendMsg(msg:String){
        channel.sendUserMessage(msg, BaseChannel.SendUserMessageHandler { userMessage, e ->
            if (e != null) {
                // Error.
                (activity as MainActivity).sToast("Error sending message!!!")
                return@SendUserMessageHandler
            }else{
                (activity as MainActivity).sToast("Sending message Success!!! \n $msg")
                rv.scrollToPosition(adapter.itemCount - 1)
                adapter.addMessage(userMessage)
                etChat.setText("")

                println("XXXASDF sender ${userMessage.sender.userId}")
            }
        })
    }

}