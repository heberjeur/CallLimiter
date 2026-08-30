package com.thirumalai.calllimiter.UI

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.thirumalai.calllimiter.R
import com.thirumalai.calllimiter.Utils.SystemBarHelper
import com.thirumalai.calllimiter.Utils.ThemeUtils

class Permissions : AppCompatActivity() {
    private lateinit var permissionContainer: LinearLayout
    private lateinit var permissionsSystemHandled: LinearLayout
    private lateinit var backBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)

        backBtn = findViewById(R.id.back_btn_permissions)

        val runtimePermissions = arrayOf(
            PermissionItem("Phone", Manifest.permission.READ_PHONE_STATE, getString(R.string.permission_1)),
            PermissionItem("Call Logs", Manifest.permission.READ_CALL_LOG, getString(R.string.permission_2)),
            PermissionItem("Contacts", Manifest.permission.READ_CONTACTS, getString(R.string.permission_3)),
            PermissionItem("Notifications", Manifest.permission.POST_NOTIFICATIONS, getString(R.string.permission_5))
        )

        val autoPermissions = arrayOf(
            AutoPermissionItem("Manage Own Calls", getString(R.string.permission_4)),
            AutoPermissionItem("Foreground Service", getString(R.string.permission_6)),
            AutoPermissionItem("Auto Restart on Boot", getString(R.string.permission_7)),
            AutoPermissionItem("Vibrate", getString(R.string.permission_8))
        )

        permissionContainer = findViewById(R.id.permission_container)

        for (item in runtimePermissions) {
            val permissionView = LayoutInflater.from(this).inflate(R.layout.permission_item, permissionContainer, false)
            val permissionTitle: TextView = permissionView.findViewById(R.id.permission_title)
            val permissionDesc: TextView = permissionView.findViewById(R.id.permission_desc)
            val permissionGranted: ImageView = permissionView.findViewById(R.id.permission_granted)
            val permissionNotGranted: Button = permissionView.findViewById(R.id.permission_not_granted)

            permissionTitle.text = item.title
            permissionDesc.text = item.description

            val granted = if (item.permission == Manifest.permission.POST_NOTIFICATIONS) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            } else {
                ContextCompat.checkSelfPermission(this, item.permission) == PackageManager.PERMISSION_GRANTED
            }

            if (granted) {
                permissionGranted.visibility = View.VISIBLE
                permissionNotGranted.visibility = View.GONE
            } else {
                permissionNotGranted.visibility = View.VISIBLE
                permissionGranted.visibility = View.GONE
                permissionNotGranted.setOnClickListener {
                    ActivityCompat.requestPermissions(
                        this@Permissions,
                        arrayOf(item.permission),
                        1001
                    )
                }
            }
            permissionContainer.addView(permissionView)
        }

        permissionsSystemHandled = findViewById(R.id.permission_system_handled)

        for (item in autoPermissions) {
            val permissionView = LayoutInflater.from(this).inflate(R.layout.permission_item, permissionsSystemHandled, false)
            val permissionTitle: TextView = permissionView.findViewById(R.id.permission_title)
            val permissionDesc: TextView = permissionView.findViewById(R.id.permission_desc)
            val permissionGranted: ImageView = permissionView.findViewById(R.id.permission_granted)
            val permissionNotGranted: Button = permissionView.findViewById(R.id.permission_not_granted)

            permissionTitle.text = item.title
            permissionDesc.text = item.description

            permissionGranted.visibility = View.VISIBLE
            permissionNotGranted.visibility = View.GONE
            permissionsSystemHandled.addView(permissionView)
        }

        backBtn.setOnClickListener {
            val intent = Intent(this@Permissions, com.thirumalai.calllimiter.UI.Settings::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            updatePermissionStatus(isGranted, permissions)
        }
    }

    private fun updatePermissionStatus(isGranted: Boolean, permissions: Array<out String>) {
        val runtimePermissions = arrayOf(
            PermissionItem("Phone", Manifest.permission.READ_PHONE_STATE, getString(R.string.permission_1)),
            PermissionItem("Call Logs", Manifest.permission.READ_CALL_LOG, getString(R.string.permission_2)),
            PermissionItem("Contacts", Manifest.permission.READ_CONTACTS, getString(R.string.permission_3)),
            PermissionItem("Notifications", Manifest.permission.POST_NOTIFICATIONS, getString(R.string.permission_5))
        )

        var permissionToBeUpdated = ""
        for (item in runtimePermissions) {
            if (permissions.isNotEmpty() && item.permission == permissions[0]) {
                permissionToBeUpdated = item.title
                break
            }
        }

        for (i in 1 until permissionContainer.childCount) {
            val permView = permissionContainer.getChildAt(i)
            val permissionGranted: ImageView = permView.findViewById(R.id.permission_granted)
            val permissionNotGranted: Button = permView.findViewById(R.id.permission_not_granted)
            val permissionTitle: TextView = permView.findViewById(R.id.permission_title)

            if (permissionToBeUpdated == permissionTitle.text.toString()) {
                if (isGranted) {
                    permissionGranted.visibility = View.VISIBLE
                    permissionNotGranted.visibility = View.GONE
                } else {
                    permissionNotGranted.visibility = View.VISIBLE
                    permissionGranted.visibility = View.GONE
                    permissionNotGranted.setOnClickListener {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        }
    }

    private data class PermissionItem(
        val title: String,
        val permission: String,
        val description: String
    )

    private data class AutoPermissionItem(
        val title: String,
        val description: String
    )
}
