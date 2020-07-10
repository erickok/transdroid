package org.transdroid.core.app.settings;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

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


    public static MaterialDialog.Builder applyDialogTheme(MaterialDialog.Builder builder) {
        SystemSettings settings = SystemSettings_.getInstance_(builder.getContext());

        if (settings.autoDarkTheme()) {
            return builder;
        }

        return builder.theme(settings.useDarkTheme() ? Theme.DARK : Theme.LIGHT);
    }
}
