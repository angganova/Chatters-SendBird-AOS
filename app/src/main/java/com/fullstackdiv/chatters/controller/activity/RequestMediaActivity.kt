package com.fullstackdiv.chatters.controller.activity

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.controller.activity.adapter.MediaListAdapter
import com.google.android.material.snackbar.Snackbar
import java.util.ArrayList
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fullstackdiv.chatters.helper.utils.PopUpUtils
import kotlinx.android.synthetic.main.activity_base_rv.*
import com.fullstackdiv.chatters.model.MediaDataModel


class RequestMediaActivity : AppCompatActivity() {
    private val TYPE_IMAGE = "images"
    private val TYPE_CONTACT = "contacts"

    private val PERMISSION_WRITE_EXTERNAL_STORAGE = 14
    private val PERMISSION_READ_CONTACTS = 15

    var data:MutableList<MediaDataModel> = arrayListOf()
    lateinit var adapter: MediaListAdapter

    var title = ""
    var type = ""

    var opt_menu = 0

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissionImages()) {
                        data = getAllData()
                        setBaseView()
                    }
                } else {
                    showSnack()
                    pb.visibility = View.GONE
                }
                return
            }

            PERMISSION_READ_CONTACTS -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissionContact()) {
                        data = getAllData()
                        setBaseView()
                    }
                } else {
                    showSnack()
                    pb.visibility = View.GONE
                }
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_rv)

        val extras = intent.extras
        type = extras?.getString("type", "")?:""

        title = "$type to send . . ."
        toolbar.title = title
        setSupportActionBar(toolbar)

        when (type.toLowerCase()){
            TYPE_IMAGE ->{
                if (checkPermissionImages()) {
                    data = getAllData()
                    setBaseView()
                }
            }
            TYPE_CONTACT ->{
                if (checkPermissionContact()) {
                    data = getAllData()
                    setBaseView()
                }
            }
            else -> finish()
        }
    }

    fun setBaseView(){
        toolbar.setNavigationIcon(R.drawable.ic_back_white)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        if (data.size>0) {
            val layoutManager = when(type.toLowerCase()){
                TYPE_IMAGE -> GridLayoutManager(this, 3)
                TYPE_CONTACT -> LinearLayoutManager(this)
                else -> LinearLayoutManager(this)
            }

            rv.layoutManager = layoutManager
            rv.itemAnimator = DefaultItemAnimator()

            adapter = MediaListAdapter(this, getAllData())
            rv.adapter = adapter

            adapter.setOnItemClickListener(object :MediaListAdapter.OnItemClickListener{
                override fun OnItemClickListener(data: MediaDataModel, position: Int) {
                    if (adapter.selection_state){
                        if (adapter.isSelected(position)) {
                            unSelectData(position)
                            if (adapter.selectedCount() == 0) endSelection()
                        } else selectData(position)

                    }
                }

            })

            adapter.setOnItemLongClickListener(object :MediaListAdapter.OnItemLongClickListener{
                override fun OnItemLongClickListener(data: MediaDataModel, position: Int) {
                    if(adapter.selection_state){
                        if (adapter.isSelected(position)) {
                            unSelectData(position)
                            if (adapter.selectedCount() == 0) endSelection()
                        } else selectData(position)
                    }else {
                        startSelection()
                        selectData(position)
                    }
                }

            })

            rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        if(layoutManager.findLastVisibleItemPosition() % 20 > 10){
                            adapter.count = adapter.count + 20
                            rv.post { adapter.notifyItemInserted(adapter.count - 1) }
                        }
                    }
                }
            })

            pb.visibility = View.GONE
        } else tvEmpty.text = getString(R.string.empty_chat_list)
    }

    private fun getAllData(): MutableList<MediaDataModel> {
        val cursor: Cursor?
        val listData = ArrayList<MediaDataModel>()

        when(type.toLowerCase()){
            TYPE_IMAGE -> {
                val uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                cursor = contentResolver.query(
                    uri, projection, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")

                val column_index_data = cursor!!.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

                while (cursor.moveToNext()){
                    val media = MediaDataModel()
                    media.uri = cursor.getString(column_index_data)
                    media.type = type
                    listData.add(media)
                }

            }

            TYPE_CONTACT -> {
                cursor = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,null, null, null,
                    null)

                while (cursor.moveToNext()){
                    val media = MediaDataModel()
//
//                    val contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.NAME_RAW_CONTACT_ID))
//                    val phones = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                                        null,
//                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
//                                        null,
//                                        null)
//                    if (phones != null) {
//                            while (phones.moveToNext()) {
//                                media.subtitle = phones.getString(phones.getColumnIndex(
//                                    ContactsContract.CommonDataKinds.Phone.NUMBER))
//                            }
//
//                        media.title = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
//                        media.type = type
//                        listData.add(media)
//                        phones.close()
//                    }

                    media.subtitle = ""
                    media.title = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    media.type = type
                    listData.add(media)
                }
            }

            else -> cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,null, null, null, null)
        }

        cursor.close()

        println("XXXASDF ${listData.size}")
        return listData
    }



    /**User Action**/
    fun startSelection(){
        setSelectionActBar(
            View.OnClickListener {
                setNormalActBar()
                endSelection()
            })
        adapter.selection_state = true
    }

    fun endSelection(){
        setNormalActBar()
        adapter.clearSelection()
    }

    fun selectData(pos: Int){
        adapter.select(pos)
        updateToolbarMenuCounter(adapter.selectedCount())
    }

    fun unSelectData(pos: Int){
        adapter.unSelect(pos)
        updateToolbarMenuCounter(adapter.selectedCount())
    }


    fun setSelectionActBar(click: View.OnClickListener){
        if (opt_menu != 0) return

        opt_menu = 1

        toolbar.setNavigationIcon(R.drawable.ic_back_white)
        toolbar.setNavigationOnClickListener(click)
        invalidateOptionsMenu()
    }

    fun setNormalActBar(){
        if (opt_menu == 0) return

        opt_menu = 0
        toolbar.title = title
        toolbar.setNavigationOnClickListener{onBackPressed()}
        invalidateOptionsMenu()
    }

    fun updateToolbarMenuCounter(count:Int){
        toolbar.title = count.toString()
        invalidateOptionsMenu()
    }


    private fun checkPermissionImages():Boolean{
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showSnack()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSION_WRITE_EXTERNAL_STORAGE
                    )
                }
            }
            return false
        }
        return true
    }

    private fun checkPermissionContact():Boolean{
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.READ_CONTACTS)) {
                showSnack()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_CONTACTS),
                        PERMISSION_READ_CONTACTS
                    )
                }
            }
            return false
        }
        return true
    }

    fun showSnack(){
        val snackbar = Snackbar.make(CLRoot,
            when(type.toLowerCase()){
                TYPE_IMAGE -> "Storage access permissions are required to upload/download files."
                TYPE_CONTACT -> "Contact Permission Needed"
                else -> ""
            }, Snackbar.LENGTH_LONG).setAction("OK")
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                        arrayOf(
                            when(type.toLowerCase()){
                                TYPE_IMAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
                                TYPE_CONTACT -> Manifest.permission.READ_CONTACTS
                                else -> ""
                        }),
                        when(type.toLowerCase()){
                            TYPE_IMAGE -> PERMISSION_WRITE_EXTERNAL_STORAGE
                            TYPE_CONTACT -> PERMISSION_READ_CONTACTS
                            else -> 0
                        }
                    )
                }
            }
        val view = snackbar.view
        val tv = view.findViewById(R.id.snackbar_text) as TextView
        tv.setTextColor(ContextCompat.getColor(this, R.color.white))
        snackbar.show()
    }


    /**OVERRIDE**/
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        return if (opt_menu == 1) {
            inflater.inflate(R.menu.select_file_menu, menu)
            super.onCreateOptionsMenu(menu)
        } else super.onCreateOptionsMenu(menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mnDone ->{
                PopUpUtils.sLongToast(this, "Ready to send")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (adapter.selection_state) {
            setNormalActBar()
            endSelection()
        }else super.onBackPressed()
    }
}


