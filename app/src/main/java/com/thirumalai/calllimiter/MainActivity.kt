package com.thirumalai.calllimiter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.thirumalai.calllimiter.BottomSheets.TimerBottomSheet
import com.thirumalai.calllimiter.Data.PreferenceHelper
import com.thirumalai.calllimiter.Service.CallMonitorService
import com.thirumalai.calllimiter.UI.OnboardingActivity
import com.thirumalai.calllimiter.UI.Settings
import com.thirumalai.calllimiter.Utils.SystemBarHelper
import com.thirumalai.calllimiter.Utils.ThemeUtils
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var setLimit: Button
    private lateinit var selectFromContacts: Button
    private var selectedHour = -1
    private var selectedMinute = -1
    private var selectedSecond = -1
    private lateinit var phoneNumberField: TextInputEditText
    private lateinit var contactNameField: TextInputEditText
    private var isPhoneAvailable = false
    private var isTimeAvailable = false
    private lateinit var settings: ImageView

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
        private const val PICK_CONTACT_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceHelper.init(this)
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)

        val isFirstTimeLogin = PreferenceHelper.isFirstTimeLogin()
        if (isFirstTimeLogin) {
            PreferenceHelper.saveBufferTime(10)
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)

        val timeLimitButton: Button = findViewById(R.id.set_time_limit_button)
        setLimit = findViewById(R.id.set_limit_button)
        selectFromContacts = findViewById(R.id.select_contact_button)
        phoneNumberField = findViewById(R.id.phone_number_input)
        contactNameField = findViewById(R.id.contact_name)
        settings = findViewById(R.id.settings_button)

        checkAndSetInitialDate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requestNecessaryPermissions()
        }

        try {
            updateSavedLimitsUI("LoadOnCreate")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        phoneNumberField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isPhoneAvailable = count != 0
            }
            override fun afterTextChanged(editable: Editable?) {}
        })

        timeLimitButton.setOnClickListener {
            val bottomSheet = TimerBottomSheet(object : TimerBottomSheet.OnTimeSelectedListener {
                override fun onTimeSelected(hours: Int, minutes: Int, seconds: Int) {
                    selectedHour = hours
                    selectedMinute = minutes
                    selectedSecond = seconds
                    setLimit.text = "SET LIMIT - $hours hrs $minutes mins $seconds secs"
                }

                override fun onTimerReset() {
                    isTimeAvailable = false
                }
            })
            bottomSheet.show(supportFragmentManager, "TimerBottomSheet")
        }

        setLimit.setOnClickListener {
            val phoneNumber = phoneNumberField.text?.toString()?.trim() ?: ""
            val contactName = contactNameField.text?.toString()?.trim() ?: ""
            if (phoneNumber.isEmpty() || selectedHour == -1 || selectedMinute == -1 || selectedSecond == -1) {
                Toast.makeText(this@MainActivity, "Please make sure phone number and time limit is set.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val totalSeconds = (selectedHour * 3600) + (selectedMinute * 60) + selectedSecond

            val jsonObject = JSONObject()
            try {
                if (contactName.isNotEmpty()) {
                    jsonObject.put("name", contactName)
                }
                jsonObject.put("limit", totalSeconds)
                jsonObject.put("remaining_time", totalSeconds)
                jsonObject.put("last_updated", getTodayDate())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            PreferenceHelper.saveContact(phoneNumber, jsonObject.toString())

            isPhoneAvailable = false
            phoneNumberField.text = null
            contactNameField.text = null
            setLimit.setText(R.string.set_limit)

            clearInputFocus()

            selectedHour = -1
            selectedMinute = -1
            selectedSecond = -1

            try {
                updateSavedLimitsUI("SetLimit")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        selectFromContacts.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_CONTACT_REQUEST)
        }

        settings.setOnClickListener {
            val intent = Intent(this@MainActivity, Settings::class.java)
            startActivity(intent)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private fun requestNecessaryPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CALL_LOG)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECEIVE_BOOT_COMPLETED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 1)
        }
    }

    @Throws(JSONException::class)
    fun updateSavedLimitsUI(action: String) {
        val savedLimitsLayout: LinearLayout = findViewById(R.id.saved_limits_layout)
        savedLimitsLayout.removeAllViews()

        val allEntries = PreferenceHelper.getAllContact() ?: emptyMap<String, Any>()
        val isLimitForEveryNumberEnabled = PreferenceHelper.getLimitForAllNumbersEnabled()

        if (action == "LoadOnCreate" && (allEntries.isNotEmpty() || isLimitForEveryNumberEnabled)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, CallMonitorService::class.java))
                Log.d("MainActivity", "Started Foreground Service from OnCreate")
            }
        } else if (action == "SetLimit" && allEntries.size == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, CallMonitorService::class.java))
                Log.d("MainActivity", "Started Foreground Service from SetLimit")
            }
        } else if (action == "DeleteTimeLimit" && allEntries.isEmpty() && !isLimitForEveryNumberEnabled) {
            val stopForegroundService = Intent(this, CallMonitorService::class.java)
            stopService(stopForegroundService)
            Log.d("MainActivity", "Stopped Foreground Service from DeleteTimeLimit")
        }

        for (entry in allEntries.entries) {
            val phoneNumber = entry.key
            val phoneNumberData = entry.value as? String

            if (phoneNumberData != null) {
                try {
                    val jsonObject = JSONObject(phoneNumberData)
                    val remainingTime = jsonObject.getInt("remaining_time")
                    var contactName = ""
                    if (jsonObject.has("name")) {
                        contactName = jsonObject.getString("name")
                    }
                    createLayout(savedLimitsLayout, phoneNumber, remainingTime, contactName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createLayout(savedLimitsLayout: LinearLayout, phoneNumber: String, remainingTime: Int, contactName: String) {
        val hours = remainingTime / 3600
        val minutes = (remainingTime % 3600) / 60
        val seconds = remainingTime % 60

        val entryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 60)
            }
        }

        val valueLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.8f)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val identifier = EditText(this).apply {
            setText(contactName.ifEmpty { phoneNumber })
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            tag = "number"
            background = null
            isFocusable = false
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT
            isFocusableInTouchMode = false
            isEnabled = false
            setTextColor(currentTextColor)
            isClickable = false
            isLongClickable = false
        }

        val wrapper = ContextThemeWrapper(this, R.style.Widget_App_Button_IconOnly)
        val iconButton = MaterialButton(wrapper, null, R.style.Widget_App_Button_IconOnly).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f)
            setBackgroundColor(Color.TRANSPARENT)
            setIconResource(R.drawable.edit_24px)
            text = ""
            strokeWidth = 0
        }

        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        iconButton.iconTint = ColorStateList.valueOf(typedValue.data)

        iconButton.setOnClickListener {
            val isCurrentlyEditing = iconButton.tag != null && iconButton.tag as Boolean

            if (!isCurrentlyEditing) {
                iconButton.tag = true
                iconButton.setIconResource(R.drawable.check_24px)
                enableEditing(identifier)
            } else {
                iconButton.tag = false
                iconButton.setIconResource(R.drawable.edit_24px)

                val nameInput = identifier.text.toString().trim()
                if (nameInput.isEmpty()) {
                    identifier.setText(phoneNumber)
                }
                val phoneNumberData = PreferenceHelper.getContact(phoneNumber)
                if (phoneNumberData != null) {
                    try {
                        val jsonObject = JSONObject(phoneNumberData)
                        jsonObject.put("name", nameInput)
                        PreferenceHelper.saveContact(phoneNumber, jsonObject.toString())
                        updateSavedLimitsUI("Refresh")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                disableEditing(identifier)
            }
        }

        identifier.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                disableEditing(identifier)
                true
            } else {
                false
            }
        }

        identifier.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            val isCurrentlyEditing = iconButton.tag != null && iconButton.tag as Boolean
            if (!hasFocus && isCurrentlyEditing) {
                iconButton.performClick()
            }
        }

        header.addView(identifier)
        header.addView(iconButton)

        val time = TextView(this).apply {
            text = "$hours hrs $minutes mins $seconds seconds"
            textSize = 16f
        }

        valueLayout.addView(header)
        valueLayout.addView(time)

        valueLayout.setOnLongClickListener {
            val isCurrentlyEditing = iconButton.tag != null && iconButton.tag as Boolean
            if (isCurrentlyEditing) {
                return@setOnLongClickListener true
            }
            val bottomSheet = TimerBottomSheet(object : TimerBottomSheet.OnTimeSelectedListener {
                override fun onTimeSelected(hours: Int, minutes: Int, seconds: Int) {
                    selectedHour = hours
                    selectedMinute = minutes
                    selectedSecond = seconds

                    val phoneNumberData = PreferenceHelper.getContact(phoneNumber)
                    if (phoneNumberData != null) {
                        try {
                            val jsonObject = updateLimit(phoneNumberData)
                            PreferenceHelper.saveContact(phoneNumber, jsonObject.toString())
                            updateSavedLimitsUI("Refresh")

                            selectedHour = -1
                            selectedMinute = -1
                            selectedSecond = -1
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                @Throws(JSONException::class)
                private fun updateLimit(phoneNumberData: String): JSONObject {
                    val jsonObject = JSONObject(phoneNumberData)
                    val remainingTime = jsonObject.getInt("remaining_time")
                    val limit = jsonObject.getInt("limit")
                    val updatedLimit = (selectedHour * 3600) + (selectedMinute * 60) + selectedSecond
                    if (limit == remainingTime || updatedLimit == remainingTime || remainingTime >= updatedLimit) {
                        jsonObject.put("limit", updatedLimit)
                        jsonObject.put("remaining_time", updatedLimit)
                    } else if (limit < updatedLimit) {
                        val newTime = updatedLimit - limit + remainingTime
                        jsonObject.put("limit", updatedLimit)
                        jsonObject.put("remaining_time", newTime)
                    }
                    return jsonObject
                }

                override fun onTimerReset() {
                    isTimeAvailable = false
                }
            }, phoneNumber, contactName)
            bottomSheet.show(supportFragmentManager, "TimerBottomSheet")
            false
        }

        val deleteButton = MaterialButton(this).apply {
            id = View.generateViewId()
            textSize = 16f
            cornerRadius = 8
            iconPadding = 0
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            setOnClickListener {
                try {
                    deleteTimeLimit(phoneNumber)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.2f)
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.delete_24px)
        }

        entryLayout.addView(valueLayout)
        entryLayout.addView(deleteButton)
        savedLimitsLayout.addView(entryLayout)
    }

    @Throws(JSONException::class)
    private fun deleteTimeLimit(phoneNumber: String) {
        PreferenceHelper.removeContact(phoneNumber)
        updateSavedLimitsUI("DeleteTimeLimit")
    }

    override fun onResume() {
        super.onResume()
        try {
            updateSavedLimitsUI("Refresh")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)
    }

    private fun checkAndSetInitialDate() {
        val currentDate = getTodayDate()
        val lastSavedDate = PreferenceHelper.getLastUpdatedDate()
        if (lastSavedDate.isEmpty()) {
            PreferenceHelper.saveLastUpdatedDate(currentDate)
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val phoneNumberUtil = PhoneNumberUtil.getInstance()

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            val contactUri = data.data ?: return
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

            try {
                contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                        val phoneNumber = cursor.getString(numberIndex)
                        val name = cursor.getString(nameIndex)

                        val numberWithoutCountryCode = if (phoneNumber.startsWith("+")) {
                            phoneNumberUtil.parse(phoneNumber, null).nationalNumber.toString()
                        } else {
                            cleanLocalPhoneNumber(phoneNumber)
                        }

                        phoneNumberField.setText(numberWithoutCountryCode)
                        contactNameField.setText(name)
                    }
                }
            } catch (e: Exception) {
                if (e.toString().contains("Error type: INVALID_COUNTRY_CODE.")) {
                    Toast.makeText(this, "Pick valid phone number.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cleanLocalPhoneNumber(rawNumber: String?): String {
        if (rawNumber == null) return ""
        return rawNumber.replace("[^\\d]".toRegex(), "")
    }

    private fun enableEditing(editText: EditText) {
        editText.isFocusableInTouchMode = true
        editText.isFocusable = true
        editText.isEnabled = true
        editText.isClickable = true
        editText.isLongClickable = true
        editText.setSelection(editText.text.length)
        editText.requestFocus()

        editText.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun disableEditing(editText: EditText) {
        editText.isFocusable = false
        editText.isFocusableInTouchMode = false
        val originalColor = editText.currentTextColor
        editText.isEnabled = false
        editText.setTextColor(originalColor)
        editText.isClickable = false
        editText.isLongClickable = false

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    private fun clearInputFocus() {
        phoneNumberField.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(phoneNumberField.windowToken, 0)

        contactNameField.clearFocus()
        imm?.hideSoftInputFromWindow(contactNameField.windowToken, 0)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextInputEditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    clearInputFocus()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
