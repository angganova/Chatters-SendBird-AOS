package com.fullstackdiv.chatters.helper.utils

import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.sendbird.android.GroupChannel
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


/**
 * Created by Angga N P on 3/29/2019.
 */

object ImageUtils {
    fun getGroupChannelImage(channel: GroupChannel): String {
        val members = channel.members
        val id = SendBird.getCurrentUser().userId

        return when {
            // Invalid Channel
            members.size < 2 || SendBird.getCurrentUser() == null -> "No Members"

            // 1 VS 1 Distinct Channel
            members.size == 2 -> {
                if (members[0].userId == id) members[1].profileUrl
                else members[0].profileUrl
            }

            // Group Distinct Channel
            else -> {
                ""
            }
        }
    }

}