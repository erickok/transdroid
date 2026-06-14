package org.transdroid.core.gui.settings;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.XmlRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceManagerBinder;
import androidx.preference.PreferenceScreen;

import org.transdroid.R;

public class PreferenceCompatActivity extends AppCompatActivity implements AppCompatCallback, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    private PreferenceFragmentCompat fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Defer until AppCompat finishes inflating the action bar into the decor view
        getWindow().getDecorView().post(this::applyEdgeToEdgeInsets);
    }

    private void applyEdgeToEdgeInsets() {
        // AppCompat positions the action bar below the status bar, leaving a transparent gap above it.
        // Setting the decor view background to green_dark fills that gap, which shows through the
        // transparent status bar in edge-to-edge mode.
        getWindow().getDecorView().setBackgroundColor(
                ContextCompat.getColor(this, R.color.green_dark));

        View content = getWindow().getDecorView().findViewById(android.R.id.content);
        if (content != null) {
            // Restore the theme's window background on the content view so the preference list
            // shows the correct background rather than the green_dark decor underneath.
            TypedValue windowBg = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.windowBackground, windowBg, true);
            if (windowBg.resourceId != 0) {
                content.setBackgroundResource(windowBg.resourceId);
            } else {
                content.setBackgroundColor(windowBg.data);
            }

            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                        insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom);
                return insets;
            });
            ViewCompat.requestApplyInsets(content);
        }
    }

    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        fragment = new RootPreferencesFragment(preferencesResId);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commitNow();
    }

    public PreferenceManager getPreferenceManager() {
        return fragment.getPreferenceManager();
    }

    public PreferenceScreen getPreferenceScreen() {
        return fragment.getPreferenceScreen();
    }

    public Preference findPreference(CharSequence key) {
        return fragment.findPreference(key);
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        LowerPreferencesFragment lowerFragment = new LowerPreferencesFragment(pref);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, lowerFragment).addToBackStack("lower").commit();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {

        private int preferencesResId;

        public RootPreferencesFragment() {
        }

        public RootPreferencesFragment(int preferencesResId) {
            this.preferencesResId = preferencesResId;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(preferencesResId);
        }
    }

    public static class LowerPreferencesFragment extends PreferenceFragmentCompat {

        private PreferenceScreen prefs;

        public LowerPreferencesFragment() {
        }

        public LowerPreferencesFragment(PreferenceScreen prefs) {
            this.prefs = prefs;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            if (prefs != null) {
                // Update the already loaded preferences with this fragment's manager to handle dialog clicks, etc.
                for (int i = 0; i < prefs.getPreferenceCount(); i++) {
                    PreferenceManagerBinder.bind(prefs.getPreference(i), getPreferenceManager());
                }
                setPreferenceScreen(prefs);
                prefs = null;
            }
        }
    }
}
