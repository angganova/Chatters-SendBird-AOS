package com.fullstackdiv.chatters.helper.utils

import android.content.Context
import android.graphics.Color
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * Created by Angga N P on 10/18/2018.
 */

class DataUtils{
    companion object {
        private const val TAG = "TAG"

        fun dpToPx(ctx: Context, dp: Int): Int {
            val r = ctx.resources
            return Math.round(
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp.toFloat(),
                    r.displayMetrics
                )
            )
        }

        fun isValidEmail(target: CharSequence): Boolean {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }

        fun urlEncode(s: String): String {
            try {
                return URLEncoder.encode(s, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException("URLEncoder.encode() failed for $s")
            }

        }


        fun genCilok(s: String): String {
            val data = s.toByteArray(StandardCharsets.UTF_8)
            val cilok = Base64.encodeToString(data, Base64.DEFAULT)

            Log.i(TAG, "genCilok: $cilok")
            Log.i(TAG, "XXX genCilok: $s")
//            Log.i(TAG, "XXX Enc genCilok: ${FcSecurity.encrypt(s)}")
            return cilok
        }


        /*ALL ABOUT TIME*/
        fun getDateFormat(s: String, opid: Int? = 0): String {
            var format = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
            val newDate: Date?
            try {
                newDate = format.parse(s)
            } catch (e: ParseException) {
                e.printStackTrace()
                return "Date not valid"
            }

            if (newDate != null) {
                when (opid) {
                    0 -> {
                        format = SimpleDateFormat("dd MMM yyyy")
                        return format.format(newDate)
                    }
                    1 -> {
                        format = SimpleDateFormat("HH:mm")
                        return format.format(newDate)
                    }
                    2 -> {
                        format = SimpleDateFormat("HH:MM, dd MMM yyyy")
                        return format.format(newDate)
                    }
                    3 -> {
                        format = SimpleDateFormat("yyyy-MM-dd")
                        return format.format(newDate)
                    }
                    4 -> {
                        format = SimpleDateFormat("MMMM, yyyy")
                        return format.format(newDate)
                    }
                }
            }
            return "Date not valid"
        }

        fun getDateFromUnix(time: Long): String {
            val today = Date(System.currentTimeMillis())
            val date = Date(time)

            val sdf = SimpleDateFormat("dd MMM")

            return sdf.format(date).toString()
        }


        /*DATA CONVERTER*/
        fun getRandomInt(min: Int, max: Int): Int {
            return ThreadLocalRandom.current().nextInt(min, max + 1)
        }

        fun getRandomLong(min: Long?, max: Long?): Long {
            return ThreadLocalRandom.current().nextLong(min!!, max!! + 1)
        }

        fun getRandomColor(): Int {
            val rnd = Random()
            val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            return color
        }


        fun formatTime(timeInMillis: Long): String {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            return dateFormat.format(timeInMillis)
        }

        fun formatTimeWithMarker(timeInMillis: Long): String {
            val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            return dateFormat.format(timeInMillis)
        }

        fun getHourOfDay(timeInMillis: Long): Int {
            val dateFormat = SimpleDateFormat("H", Locale.getDefault())
            return Integer.valueOf(dateFormat.format(timeInMillis))
        }

        fun getMinute(timeInMillis: Long): Int {
            val dateFormat = SimpleDateFormat("m", Locale.getDefault())
            return Integer.valueOf(dateFormat.format(timeInMillis))
        }

        fun formatDateTime(timeInMillis: Long): String {
            return if (isToday(timeInMillis)) {
                formatTime(timeInMillis)
            } else {
                formatDate(timeInMillis)
            }
        }

        fun formatDate(timeInMillis: Long): String {
            val dateFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())
            if (isToday(timeInMillis)) return "Today"
            return dateFormat.format(timeInMillis)
        }

        fun isToday(timeInMillis: Long): Boolean {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val date = dateFormat.format(timeInMillis)
            return date == dateFormat.format(System.currentTimeMillis())
        }

        fun hasSameDate(millisFirst: Long, millisSecond: Long): Boolean {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            return dateFormat.format(millisFirst) == dateFormat.format(millisSecond)
        }

    }
}