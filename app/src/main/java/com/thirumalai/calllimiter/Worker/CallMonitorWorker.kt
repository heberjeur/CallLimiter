package com.thirumalai.calllimiter.Worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.thirumalai.calllimiter.Service.CallMonitorService

class CallMonitorWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val context = applicationContext
        val serviceIntent = Intent(context, CallMonitorService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        Log.d("BootReceiver", "Started Foreground Service from Boot Receiver")
        return Result.success()
    }
}
