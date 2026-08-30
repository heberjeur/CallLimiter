package com.thirumalai.calllimiter.Utils;

import android.app.Activity;
import androidx.appcompat.app.AppCompatDelegate;
import com.thirumalai.calllimiter.Data.PreferenceHelper;
import com.thirumalai.calllimiter.R;

public class ThemeUtils {
    public static void applyTheme(Activity activity) {
        String theme = PreferenceHelper.getTheme();
        if ("OLED".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            activity.setTheme(R.style.AppTheme_Oled);
        } else if ("Dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            activity.setTheme(R.style.AppTheme);
        } else if ("Light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            activity.setTheme(R.style.AppTheme);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            activity.setTheme(R.style.AppTheme);
        }
    }
}
