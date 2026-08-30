package com.thirumalai.calllimiter.BroadcastReceivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.thirumalai.calllimiter.Data.PreferenceHelper
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeChangeReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        PreferenceHelper.init(context)
        resetTime(context)
        Log.d("TimeChangeReceiver", "Time Reset")
    }

    fun resetTime(context: Context) {
        updateDate(context)

        val all = PreferenceHelper.getAllContact() ?: return
        for (phoneNumber in all.keys) {
            try {
                val dataStr = all[phoneNumber] as? String ?: continue
                val jsonObject = JSONObject(dataStr)
                val limit = jsonObject.getInt("limit")
                jsonObject.put("remaining_time", limit)
                PreferenceHelper.saveContact(phoneNumber, jsonObject.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateDate(context: Context) {
        val currentDate = getTodayDate()
        val lastUpdated = PreferenceHelper.getLastUpdatedDate()

        if (lastUpdated.isNotEmpty() && lastUpdated != currentDate) {
            PreferenceHelper.saveLastUpdatedDate(currentDate)
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }
}
