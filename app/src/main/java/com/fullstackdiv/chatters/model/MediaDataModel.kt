package com.fullstackdiv.chatters.model

import com.sendbird.android.shadow.com.google.gson.annotations.Expose
import com.sendbird.android.shadow.com.google.gson.annotations.SerializedName

/**
 * Created by Angga N P on 4/8/2019.
 */
class MediaDataModel {

    @SerializedName("uri")
    @Expose
    var uri: String? = null
    @SerializedName("id")
    @Expose
    var id: String? = null
    @SerializedName("title")
    @Expose
    var title: String? = null
    @SerializedName("subtitle")
    @Expose
    var subtitle: String? = null
    @SerializedName("type")
    @Expose
    var type: String? = null
    @SerializedName("size")
    @Expose
    var size: String? = null
    @SerializedName("image")
    @Expose
    var image: String? = null
    @SerializedName("date")
    @Expose
    var date: String? = null

    var selected:Boolean = false
}