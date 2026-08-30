package com.thirumalai.calllimiter.UI

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.thirumalai.calllimiter.R
import com.thirumalai.calllimiter.Utils.SystemBarHelper
import com.thirumalai.calllimiter.Utils.ThemeUtils

class About : AppCompatActivity() {
    private lateinit var version: TextView
    private lateinit var backBtn: ImageView
    private lateinit var sourceCode: CardView
    private lateinit var changeLog: CardView
    private lateinit var termsAndConditions: CardView
    private lateinit var privacyPolicy: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)

        version = findViewById(R.id.version)
        sourceCode = findViewById(R.id.source_code)
        termsAndConditions = findViewById(R.id.terms_conditions_about)
        privacyPolicy = findViewById(R.id.privacy_policy_about)
        changeLog = findViewById(R.id.change_log)
        backBtn = findViewById(R.id.back_btn_about)

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            version.text = "Version $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        sourceCode.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Thiru-Malai/CallLimiter"))
            startActivity(intent)
        }

        changeLog.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Thiru-Malai/CallLimiter/releases"))
            startActivity(intent)
        }

        termsAndConditions.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Thiru-Malai/CallLimiter/blob/master/TermsAndConditions.md"))
            startActivity(intent)
        }

        privacyPolicy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Thiru-Malai/CallLimiter/blob/master/PrivacyPolicy.md"))
            startActivity(intent)
        }

        backBtn.setOnClickListener {
            val intent = Intent(this@About, Settings::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
}
