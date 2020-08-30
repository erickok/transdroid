package org.transdroid.core.gui.settings;

import android.os.Bundle;

import androidx.annotation.XmlRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatCallback;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceManagerBinder;
import androidx.preference.PreferenceScreen;

public class PreferenceCompatActivity extends AppCompatActivity implements AppCompatCallback, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    private PreferenceFragmentCompat fragment;

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
