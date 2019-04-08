package com.fullstackdiv.chatters.controller.activity.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fullstackdiv.chatters.R
import com.fullstackdiv.chatters.model.MediaDataModel
import com.squareup.picasso.Picasso
import java.io.File


/**
 * Created by Angga N P on 3/27/2019.
 */

class MediaListAdapter(val mContext: Context, val data: List<MediaDataModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val TYPE_IMAGE = 100
    val TYPE_CONTACTS = 200

    private var itemClickListener: OnItemClickListener? = null
    private var itemLongClickListener: OnItemLongClickListener? = null

    private val picasso: Picasso = Picasso.with(mContext)
    var count = 20

    var selection_state = false

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        itemLongClickListener = listener
    }

    interface OnItemLongClickListener {
        fun OnItemLongClickListener(data: MediaDataModel, position: Int)
    }

    interface OnItemClickListener {
        fun OnItemClickListener(data: MediaDataModel, position: Int)
    }

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view){
        var ivPic: ImageView = view.findViewById(R.id.ivPic)
        var ivSelected: ImageView = view.findViewById(R.id.ivSelected)

        fun bindView(media:MediaDataModel, position: Int,
                     clickListener: OnItemClickListener?,
                     longClickListener: OnItemLongClickListener?){
            val file = File(media.uri)
            picasso.load(file)
                .resize(200, 200)
                .centerCrop()
                .into(ivPic)

            if (media.selected) ivSelected.visibility = View.VISIBLE
            else ivSelected.visibility = View.GONE

            if (clickListener != null) {
                itemView.setOnClickListener { clickListener.OnItemClickListener(media, position) }
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.OnItemLongClickListener(media, position)
                    true
                }
            }
        }
    }

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view){
        var tvTitle: TextView = view.findViewById(R.id.tvTitle)
        var tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        var ivSelected: ImageView = view.findViewById(R.id.ivSelected)
        var vSelected: View = view.findViewById(R.id.vSelected)

        fun bindView(media:MediaDataModel, position: Int,
                     clickListener: OnItemClickListener?,
                     longClickListener: OnItemLongClickListener?){
            tvTitle.text = media.title
            tvSubtitle.text = media.subtitle

            if (media.selected) {
                ivSelected.visibility = View.VISIBLE
                vSelected.visibility = View.VISIBLE
            }
            else {
                ivSelected.visibility = View.GONE
                vSelected.visibility = View.GONE
            }

            if (clickListener != null) {
                itemView.setOnClickListener { clickListener.OnItemClickListener(media, position) }
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener {
                    longClickListener.OnItemLongClickListener(media, position)
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (count<data.size) count else data.size
    }

    override fun getItemViewType(position: Int): Int {
        return when(data[position].type!!.toLowerCase()){
            "images" -> TYPE_IMAGE
            "contacts" -> TYPE_CONTACTS
            else -> TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_IMAGE -> ImageViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attachment_image_grid, parent, false))

            TYPE_CONTACTS -> ContactViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attachment_contact_list, parent, false))

            else -> ImageViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attachment_image_grid, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_IMAGE -> (holder as ImageViewHolder)
                .bindView(data[position], position,
                    itemClickListener, itemLongClickListener)
            TYPE_CONTACTS -> (holder as ContactViewHolder)
                .bindView(data[position], position,
                    itemClickListener, itemLongClickListener)
        }
    }


    // Item Selection
    fun getSelectedMedia():List<MediaDataModel>{
        val data = arrayListOf<MediaDataModel>()
        for (x in data){
            if (x.selected) data.add(x)
        }

        return data
    }

    fun select(pos: Int){
        selection_state = true
        data[pos].selected = true
        notifyItemChanged(pos)
    }

    fun unSelect(pos: Int){
        data[pos].selected = false
        notifyItemChanged(pos)
    }

    fun isSelected(pos: Int): Boolean{
        return data[pos].selected
    }

    fun clearSelection(){
        selection_state = false
        for (x in data) {
            x.selected = false
        }
        notifyDataSetChanged()
    }

    fun selectedCount():Int{
        var count = 0
        for (x in data){
            if (x.selected) count+=1
        }
        return count
    }
}