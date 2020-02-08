package org.transdroid.core.app.settings;


import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

public class SettingsUtils {
    /**
     * Set the theme according to the user preference.
     */
    public static void applyDayNightTheme(AppCompatActivity activity) {
        SystemSettings settings = SystemSettings_.getInstance_(activity);

        if (settings.autoDarkTheme()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(settings.useDarkTheme() ?
                    AppCompatDelegate.MODE_NIGHT_YES :
                    AppCompatDelegate.MODE_NIGHT_NO
            );
        }
    }
}
