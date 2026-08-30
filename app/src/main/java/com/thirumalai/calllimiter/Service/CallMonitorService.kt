package com.thirumalai.calllimiter.Service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.thirumalai.calllimiter.BroadcastReceivers.CancelTimerReceiver
import com.thirumalai.calllimiter.Data.PreferenceHelper
import com.thirumalai.calllimiter.MainActivity
import com.thirumalai.calllimiter.R
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallMonitorService : Service() {
    private val channelId = "CallMonitorChannel"
    private var telephonyManager: TelephonyManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var endCallRunnable: Runnable? = null
    private var callTimeLimit = 10 * 1000 // In milliseconds
    private var phoneStateListener: PhoneStateListener? = null
    private lateinit var phoneNumberUtil: PhoneNumberUtil
    private var isTimerRunning = false
    private var isReminderOnlyMode = false
    private val triggeredWarnings = mutableSetOf<Int>()
    private var elapsedTime = 1 // In seconds
    private var pendingIntent: PendingIntent? = null
    private var wasInCall = false

    companion object {
        private var instance: CallMonitorService? = null

        @JvmStatic
        fun getInstance(): CallMonitorService? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        PreferenceHelper.init(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        phoneNumberUtil = PhoneNumberUtil.getInstance()

        val clickIntent = Intent(this, CancelTimerReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, getNotification(), FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                val rawNumber = phoneNumber ?: return

                Log.d("CallMonitorService", "Phone state changed: $state for $rawNumber")
                try {
                    var numberWithoutCountryCode = rawNumber
                    if (rawNumber.startsWith("+")) {
                        val parsedNumber = phoneNumberUtil.parse(rawNumber, null)
                        numberWithoutCountryCode = parsedNumber.nationalNumber.toString()
                    }

                    if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                        wasInCall = true
                        elapsedTime = 1

                        var phoneNumberData = PreferenceHelper.getContact(numberWithoutCountryCode)
                        val isTimeLimitForAllNumbersEnabled = PreferenceHelper.getLimitForAllNumbersEnabled()
                        val isWarningReminderEnabled = PreferenceHelper.getWarningReminderEnabled()

                        if (phoneNumberData == null && isTimeLimitForAllNumbersEnabled) {
                            val newNumber = JSONObject().apply {
                                put("remaining_time", PreferenceHelper.getTimeLimitForAllNumbers())
                                put("limit", PreferenceHelper.getTimeLimitForAllNumbers())
                                put("last_updated", getTodayDate())
                            }
                            PreferenceHelper.saveContact(numberWithoutCountryCode, newNumber.toString())
                            phoneNumberData = newNumber.toString()
                            Log.d("CallMonitorService", "Added new number since limit for all numbers is enabled")
                        }

                        if (phoneNumberData != null) {
                            // Hard limit is active (with or without reminder)
                            isReminderOnlyMode = false
                            val jsonObject = JSONObject(phoneNumberData)
                            var remainingTime = jsonObject.getInt("remaining_time")
                            val bufferTime = PreferenceHelper.getBufferTime()
                            if (remainingTime < 10) {
                                remainingTime = bufferTime
                            }

                            callTimeLimit = remainingTime * 1000
                            val isCallStartBufferEnabled = PreferenceHelper.getCallStartBufferValue()
                            if (isCallStartBufferEnabled) {
                                callTimeLimit += 10000
                            }
                            startCallTimer(hasHardLimit = true)
                            Log.d("CallMonitorService", "Call started. Starting limit timer: ${callTimeLimit / 1000}s")
                        } else if (isWarningReminderEnabled) {
                            // Reminder only mode: no hard limit, alerts at intervals without cutting
                            isReminderOnlyMode = true
                            callTimeLimit = 0
                            startCallTimer(hasHardLimit = false)
                            Log.d("CallMonitorService", "Call started in Reminder Only mode")
                        } else {
                            Log.d("CallMonitorService", "No limit and no reminder active for this call")
                        }
                    } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                        if (wasInCall) {
                            wasInCall = false
                            Log.d("CallMonitorService", "Call State Idle Triggered")

                            if (isTimerRunning) {
                                stopCallTimer()
                            }

                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            if (vibrator?.hasVibrator() == true) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(200)
                                }
                            }

                            val phoneNumberData = PreferenceHelper.getContact(numberWithoutCountryCode)
                            if (phoneNumberData != null && !isReminderOnlyMode) {
                                try {
                                    val jsonObject = JSONObject(phoneNumberData)
                                    val isLimitResetEachCall = PreferenceHelper.getLimitForEachCallValue()

                                    if (!isLimitResetEachCall) {
                                        var remainingTime = (callTimeLimit / 1000) - elapsedTime
                                        if (remainingTime < 0) {
                                            remainingTime = 0
                                        }
                                        jsonObject.put("remaining_time", remainingTime)
                                    }
                                    PreferenceHelper.saveContact(numberWithoutCountryCode, jsonObject.toString())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                } catch (e: NumberParseException) {
                    Log.e("CallMonitorService", "Error during parsing phone number: ${e.message}")
                } catch (e: JSONException) {
                    Log.e("CallMonitorService", "JSON error: ${e.message}")
                }
            }
        }
        @Suppress("DEPRECATION")
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        return START_STICKY
    }

    private fun startCallTimer(hasHardLimit: Boolean) {
        if (isTimerRunning) return
        isTimerRunning = true
        triggeredWarnings.clear()

        if (hasHardLimit) {
            endCallRunnable = Runnable { endCall() }
            endCallRunnable?.let { handler.postDelayed(it, callTimeLimit.toLong()) }
        } else {
            endCallRunnable = null
        }
        handler.post(updateRunnable)
    }

    fun stopCallTimer() {
        isTimerRunning = false
        triggeredWarnings.clear()
        endCallRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacks(updateRunnable)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(1, getNotification())
    }

    private val updateRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                elapsedTime++

                val isWarningReminderEnabled = PreferenceHelper.getWarningReminderEnabled()
                val warningThresholds = PreferenceHelper.getWarningReminderThresholds()

                if (isWarningReminderEnabled) {
                    if (isReminderOnlyMode) {
                        // In reminder-only mode: alert at elapsed time milestones (or repeating every milestone)
                        if (warningThresholds.contains(elapsedTime) && !triggeredWarnings.contains(elapsedTime)) {
                            triggeredWarnings.add(elapsedTime)
                            triggerWarningAlert()
                        }
                    } else {
                        // In hard-limit mode: alert at remaining time thresholds before cut
                        val remainingSeconds = (callTimeLimit / 1000) - elapsedTime
                        if (warningThresholds.contains(remainingSeconds) && !triggeredWarnings.contains(remainingSeconds)) {
                            triggeredWarnings.add(remainingSeconds)
                            triggerWarningAlert()
                        }
                    }
                }

                updateTimerNotification()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun triggerWarningAlert() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            handler.postDelayed({
                try {
                    toneGenerator.release()
                } catch (ignored: Exception) {}
            }, 500)
        } catch (e: Exception) {
            Log.e("CallMonitorService", "Error playing warning tone: ${e.message}")
        }

        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 150, 100, 150)
                    val vibrationEffect = VibrationEffect.createWaveform(timings, -1)
                    vibrator.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
                }
            }
        } catch (e: Exception) {
            Log.e("CallMonitorService", "Error triggering vibration: ${e.message}")
        }
    }

    private fun endCall() {
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                @Suppress("DEPRECATION")
                val success = telecomManager.endCall()
                Log.d("CallMonitorService", "Call ended: $success")
            } else {
                Log.e("CallMonitorService", "TelecomManager not available or Android version too low.")
            }
        } catch (e: Exception) {
            Log.e("CallMonitorService", "Error ending call: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Call Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun getNotification(): Notification {
        val appOpenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val intent = PendingIntent.getActivity(this, 0, appOpenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Call Monitor Active")
            .setContentText("Monitoring call duration")
            .setSmallIcon(R.drawable.logo___notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_v2))
            .setContentIntent(intent)
            .build()
    }

    private fun updateTimerNotification() {
        val contentText = if (isReminderOnlyMode) {
            "Call Elapsed Time: " + formatTime(elapsedTime)
        } else {
            "Time Left: " + formatTime((callTimeLimit / 1000) - elapsedTime)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tap here to stop call timer")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.logo___notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_v2))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(1, notification)
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Int): String {
        val safeSeconds = if (seconds < 0) 0 else seconds
        val h = safeSeconds / 3600
        val m = (safeSeconds % 3600) / 60
        val s = safeSeconds % 60

        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (telephonyManager != null && phoneStateListener != null) {
            @Suppress("DEPRECATION")
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }
}
