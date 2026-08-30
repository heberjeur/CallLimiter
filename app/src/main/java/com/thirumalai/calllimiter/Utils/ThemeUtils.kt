package com.thirumalai.calllimiter.Utils

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import com.thirumalai.calllimiter.Data.PreferenceHelper
import com.thirumalai.calllimiter.R

object ThemeUtils {
    @JvmStatic
    fun applyTheme(activity: Activity) {
        val theme = PreferenceHelper.getTheme()
        when (theme) {
            "OLED" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.setTheme(R.style.AppTheme_Oled)
            }
            "Dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.setTheme(R.style.AppTheme)
            }
            "Light" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                activity.setTheme(R.style.AppTheme)
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                activity.setTheme(R.style.AppTheme)
            }
        }
    }
}
