package com.fullstackdiv.chatters.controller.fragment.main

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.controller.activity.ChatDetailActivity
import com.fullstackdiv.chatters.controller.fragment.main.adapter.ChannelListAdapter
import com.sendbird.android.*
import kotlinx.android.synthetic.main.base_rv.*
import com.sendbird.android.GroupChannel
import com.fullstackdiv.chatters.controller.activity.MainActivity
import com.fullstackdiv.chatters.helper.utils.PopUpUtils
import com.sendbird.android.ConnectionManager
import android.view.*
import androidx.core.content.ContextCompat
import com.fullstackdiv.chatters.helper.utils.DialogUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers


/**
 * Created by Angga N P on 3/27/2019.
 */

class ChannelListFragment: Fragment(){
    private val compositeDisposable = CompositeDisposable()

    private val CON_HANDLER_ID = "CON_HANDLER_ID"
    private val CH_HANDLER_ID = "CH_HANDLER_ID"
    private val CH_QUERY_LIMIT = 15

    lateinit var adapter : ChannelListAdapter
    lateinit var channelListQuery: GroupChannelListQuery

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.base_rv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ChannelListAdapter(activity!!, R.layout.item_channel_list)
    }

    fun setBaseView(){
        if (adapter.data.isNotEmpty()) {
            val layoutManager = LinearLayoutManager(this.context)
            rv.layoutManager = layoutManager
            rv.itemAnimator = DefaultItemAnimator()
            rv.adapter = adapter

            adapter.setOnItemClickListener(object : ChannelListAdapter.OnItemClickListener {
                override fun onItemClick(channel: GroupChannel, position: Int) {
                    if (adapter.selection_state){
                            if (adapter.isSelected(position)) {
                                unSelectChannel(position)
                                if (adapter.selectedCount() == 0) endSelection()
                            } else selectChannel(position)

                    }else {
                        val intent = Intent(activity!!, ChatDetailActivity::class.java)
                        intent.putExtra("url", channel.url)
                        startActivity(intent)
                    }
                }
            })

            adapter.setOnItemLongClickListener(object : ChannelListAdapter.OnItemLongClickListener {
                override fun onItemLongClick(channel: GroupChannel, position: Int) {
                    if (adapter.selection_state) {
                        if (adapter.isSelected(position)) {
                            unSelectChannel(position)
                            if (adapter.selectedCount() == 0) endSelection()
                        } else selectChannel(position)
                    } else {
                        startSelection()
                        selectChannel(position)
                    }
                }
            })

            // If user scrolls to bottom of the list, loads more channels.
            rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (layoutManager.findLastVisibleItemPosition() == adapter.itemCount - 1) {
                        loadNextChannelList()
                    }
                }
            })
        } else tvEmpty.text = getString(R.string.empty_chat_list)

        pb.visibility = View.GONE
        setChannelHandler()
    }


    fun setChannelHandler(){
        SendBird.addChannelHandler(CH_HANDLER_ID, object : SendBird.ChannelHandler() {
            override fun onMessageReceived(baseChannel: BaseChannel, baseMessage: BaseMessage) {}

            override fun onChannelChanged(channel: BaseChannel) {
                adapter.clearMap()
                adapter.updateOrInsert(channel)
            }

            override fun onTypingStatusUpdated(channel: GroupChannel?) {
                adapter.notifyDataSetChanged()
            }
        })

    }

    fun refreshData(){
        getChannel(CH_QUERY_LIMIT)
    }

    fun getChannel(limit:Int){
        channelListQuery = GroupChannel.createMyGroupChannelListQuery()
        channelListQuery.limit = limit
        channelListQuery.next(GroupChannelListQuery.GroupChannelListQueryResultHandler { list, e ->
            if (e != null) {
                // Error.
                pb.visibility = View.GONE
                tvEmpty.text = getString(R.string.err_chat_list)
                return@GroupChannelListQueryResultHandler
            }else{
                adapter.clearMap()

                for (channel in list) {
                    if(channel.memberCount>1) adapter.addChannel(channel)
                }

                if (activity != null && isAdded) setBaseView()
            }
        })
    }


    private fun loadNextChannelList() {
        channelListQuery.next(GroupChannelListQuery.GroupChannelListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                e.printStackTrace()
                return@GroupChannelListQueryResultHandler
            }

            for (channel in list) {
                if(channel.memberCount>1) adapter.addChannel(channel)
            }
        })
    }

    private fun togglePNSelectedChannel(on: Boolean) {
        compositeDisposable.add (
            Observable.fromIterable(adapter.getSelectedChannels())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<GroupChannel>() {
                    override fun onComplete() {
                        PopUpUtils.sLongToast(activity!!,
                            if (on) "Push notifications have been turned ON"
                            else "Push notifications have been turned OFF")
                    }

                    override fun onNext(channel: GroupChannel) {
                        setChannelPushPreferences(channel, on)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                })
        )
    }

    private fun leaveSelectedChannel() {
        compositeDisposable.add (
            Observable.fromIterable(adapter.getSelectedChannels())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<GroupChannel>() {
                    override fun onComplete() {
                        // Re-query message list
                        getChannel(CH_QUERY_LIMIT)
                    }

                    override fun onNext(channel: GroupChannel) {
                        channel.leave(GroupChannel.GroupChannelLeaveHandler { e ->
                            if (e != null) {
                                // Error!
                                return@GroupChannelLeaveHandler
                            }
                        })
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }

                })
        )
    }

    // Push Notification Setting
    private fun setChannelPushPreferences(channel: GroupChannel, on: Boolean) {
        // Change push preferences.
        channel.setPushPreference(on, GroupChannel.GroupChannelSetPushPreferenceHandler { e ->
            if (e != null) {
                e.printStackTrace()
                PopUpUtils.sLongToast(activity!!, "Error: " + e.message)
                return@GroupChannelSetPushPreferenceHandler
            }
        })
    }


    /**User Action**/
    fun startSelection(){
        (activity as MainActivity).setSelectionActBar(
            View.OnClickListener {
                (activity as MainActivity).setNormalActBar()
                endSelection()
            })
        adapter.selection_state = true
    }

    fun endSelection(){
        (activity as MainActivity).setNormalActBar()
        adapter.clearSelection()
    }

    fun selectChannel(pos: Int){
        adapter.select(pos)
        (activity as MainActivity).updateToolbarMenuCounter(adapter.selectedCount())
    }

    fun unSelectChannel(pos: Int){
        adapter.unSelect(pos)
        (activity as MainActivity).updateToolbarMenuCounter(adapter.selectedCount())
    }


    /**OVERRIDE**/
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        if ((activity as MainActivity).opt_menu == 1){
            inflater.inflate(R.menu.channel_menu, menu)
            if (!adapter.selection_state_on){
                menu.findItem(R.id.mnPN).icon =
                    ContextCompat.getDrawable(activity!!,
                        R.drawable.ic_pn_off_white)
            }else{
                menu.findItem(R.id.mnPN).icon =
                    ContextCompat.getDrawable(activity!!,
                        R.drawable.ic_pn_on_white)
            }

            return super.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mnDelete ->{
                DialogUtils.showDialog2(activity!!,
                    "Delete Chat ?", "Delete",
                    DialogInterface.OnClickListener{dialog, _ ->
                        leaveSelectedChannel()
                        endSelection()
                        dialog.dismiss()
                    })
                return true
            }
            R.id.mnPN ->{
                if (!adapter.selection_state_on){
                    DialogUtils.showDialog2(activity!!,
                        "Turn On Notification?", "Turn On",
                        DialogInterface.OnClickListener{dialog, _ ->
                            togglePNSelectedChannel(true)
                            endSelection()
                            dialog.dismiss()
                        })
                }else{
                    DialogUtils.showDialog2(activity!!,
                        "Turn Off Notification?", "Turn Off",
                        DialogInterface.OnClickListener{dialog, _ ->
                            togglePNSelectedChannel(false)
                            endSelection()
                            dialog.dismiss()
                        })
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        adapter.save()

        Log.d("LIFECYCLE", "GroupChannelListFragment onPause()")

        ConnectionManager.removeNetworkHandler(CON_HANDLER_ID)
        SendBird.removeChannelHandler(CH_HANDLER_ID)
    }

    override fun onResume() {
        super.onResume()

        ConnectionManager.addNetworkHandler(CON_HANDLER_ID,
            object : ConnectionManager.NetworkHandler() {
                override fun onReconnected() {
                    refreshData()
                }
            }
        )

        adapter.load()
        getChannel(CH_QUERY_LIMIT)

        setChannelHandler()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}