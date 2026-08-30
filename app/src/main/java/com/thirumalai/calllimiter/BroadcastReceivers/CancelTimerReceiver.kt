package com.thirumalai.calllimiter.BroadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thirumalai.calllimiter.Service.CallMonitorService

class CancelTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = CallMonitorService.getInstance()
        service?.stopCallTimer()
    }
}
