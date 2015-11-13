/* 
 * Copyright 2010-2013 Eric Kok et al.
 * 
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.gui.settings;

import java.util.HashMap;
import java.util.Map;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;

/**
 * Abstract activity that helps implement a preference screen for key-bound settings, i.e. settings of which there can
 * be multiple and which are identified by an ascending order number/unique key. A typical implementation calls
 * {@link #init(int, int)} during the {@link #onCreate(android.os.Bundle)} (but after calling super.onCreate(Bundle))
 * and then call initXPreference for each contained preference. {@link #onPreferencesChanged()} can be overridden to
 * react to preference changes, e.g. when field availability should be updated (and where preference dependency isn't
 * enough).
 * @author Eric Kok
 */
@EActivity
public abstract class KeyBoundPreferencesActivity extends PreferenceCompatActivity {

	@Extra
	protected int key = -1;

	private SharedPreferences sharedPrefs;
	private Map<String, String> originalSummaries = new HashMap<>();

	/**
	 * Should be called during the activity {@link #onCreate(android.os.Bundle)} (but after super.onCreate(Bundle)) to
	 * load the preferences for this screen from an XML resource.
	 * @param preferencesResId The XML resource to read preferences from, which may contain embedded
	 *            {@link PreferenceScreen} objects
	 * @param currentMaxKey The value of what is currently the last defined settings object, or -1 of no settings were
	 *            defined so far at all
	 */
	@SuppressWarnings("deprecation")
	protected final void init(int preferencesResId, int currentMaxKey) {

		// Load the raw preferences to show in this screen
		addPreferencesFromResource(preferencesResId);
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// If no key was supplied (in the extra bundle) then use a new key instead
		if (key < 0) {
			key = currentMaxKey + 1;
		}

	}

	protected void onResume() {
		super.onResume();
		// Monitor preference changes
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(
				onPreferenceChangeListener);
	}

	protected void onPause() {
		super.onPause();
		// Stop monitoring preference changes
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(
				onPreferenceChangeListener);
	}

	private OnSharedPreferenceChangeListener onPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			showValueOnSummary(key);
			onPreferencesChanged();
		}
	};

	/**
	 * Key-bound preference activities may override this method if they want to react to preference changes.
	 */
	protected void onPreferencesChanged() {
	}

	/**
	 * Updates a preference that allows for text entry via a dialog. This is used for both string and integer values. No
	 * default value will be shown.
	 * @param baseName The base name of the stored preference, e.g. item_name, which will then actually be stored under
	 *            item_name_[key]
	 * @return The concrete {@link EditTextPreference} that is bound to this preference
	 */
	protected final EditTextPreference initTextPreference(String baseName) {
		return initTextPreference(baseName, null);
	}

	/**
	 * Updates a preference that allows for text entry via a dialog. This is used for both string and integer values.
	 * @param baseName The base name of the stored preference, e.g. item_name, which will then actually be stored under
	 *            item_name_[key]
	 * @param defValue The default value for this preference, as shown when no value was yet stored
	 * @return The concrete {@link EditTextPreference} that is bound to this preference
	 */
	protected final EditTextPreference initTextPreference(String baseName, String defValue) {
		return initTextPreference(baseName, defValue, null);
	}

	/**
	 * Updates a preference (including dependency) that allows for text entry via a dialog. This is used for both string
	 * and integer values.
	 * @param baseName The base name of the stored preference, e.g. item_name, which will then actually be stored under
	 *            item_name_[key]
	 * @param defValue The default value for this preference, as shown when no value was yet stored
	 * @param dependency The base name of the preference to which this preference depends
	 * @return The concrete {@link EditTextPreference} that is bound to this preference
	 */
	@SuppressWarnings("deprecation")
	protected final EditTextPreference initTextPreference(String baseName, String defValue, String dependency) {
		// Update the loaded Preference with the actual preference key to load/store with
		EditTextPreference pref = (EditTextPreference) findPreference(baseName);
		pref.setKey(baseName + "_" + key);
		pref.setDependency(dependency == null ? null : dependency + "_" + key);
		// Update the Preference by loading the current stored value into the EditText, if it exists
		pref.setText(sharedPrefs.getString(baseName + "_" + key, defValue));
		// Remember the original descriptive summary and if we have a value, show that instead
		originalSummaries.put(baseName + "_" + key, pref.getSummary() == null ? null : pref.getSummary().toString());
		showValueOnSummary(baseName + "_" + key);
		return pref;
	}

	/**
	 * Updates a preference that simply shows a check box. No default value will be shown.
	 * @param baseName The base name of the stored preference, e.g. item_name, which will then actually be stored under
	 *            item_name_[key]
	 * @return The concrete {@link CheckBoxPreference} that is bound to this preference
	 */
	protected final CheckBoxPreference initBooleanPreference(String baseName) {
		return initBooleanPreference(baseName, false);
	}

	/**
	 * Updates a preference that simply shows a check box.
	 * @param baseName The base name of the stored preference, e.g. item_name, which will then actually be stored under
	 *            item_name_[key]
	 * @param defValue The default value for this preference, as shown when no value was yet stored
	 * @return The concrete {@link CheckBoxPreference} that is bound to this preference
	 */
	protected final CheckBoxPreference initBooleanPreference(String baseName, boolean defValue) {
		return initBooleanPreference(baseName, defValue, null);
	}

	/**
	 * Updates a preference (including dependency) that simply shows a check box.
	 * @param baseName The base name of the stored preference, e.g. item_name, which will then actually be stored under
	 *            item_name_[key]
	 * @param defValue The default value for this preference, as shown when no value was yet stored
	 * @param dependency The base name of the preference to which this preference depends
	 * @return The concrete {@link CheckBoxPreference} that is bound to this preference
	 */
	@SuppressWarnings("deprecation")
	protected final CheckBoxPreference initBooleanPreference(String baseName, boolean defValue, String dependency) {
		// Update the loaded Preference with the actual preference key to load/store with
		CheckBoxPreference pref = (CheckBoxPreference) findPreference(baseName);
		pref.setKey(baseName + "_" + key);
		pref.setDependency(dependency == null ? null : dependency + "_" + key);
		// Update the Preference by loading the current stored value into the Checkbox, if it exists
		pref.setChecked(sharedPrefs.getBoolean(baseName + "_" + key, defValue));
		return pref;
	}

	/**
	 * Updates a preference that allows picking an item from a list. No default value will be shown.
	 * @param baseName The base name of the stored preference, e.g. item_name, which will then actually be stored under
	 *            item_name_[key]
	 * @return The concrete {@link ListPreference} that is bound to this preference
	 */
	protected final ListPreference initListPreference(String baseName) {
		return initListPreference(baseName, null);
	}

	/**
	 * Updates a preference that allows picking an item from a list.
	 * @param baseName The base name of the stored preference, e.g. item_name, which will then actually be stored under
	 *            item_name_[key]
	 * @param defValue The default value for this preference, as shown when no value was yet stored
	 * @return The concrete {@link ListPreference} that is bound to this preference
	 */
	@SuppressWarnings("deprecation")
	protected final ListPreference initListPreference(String baseName, String defValue) {
		// Update the loaded Preference with the actual preference key to load/store with
		ListPreference pref = (ListPreference) findPreference(baseName);
		pref.setKey(baseName + "_" + key);
		// Update the Preference by selecting the current stored value in the list, if it exists
		pref.setValue(sharedPrefs.getString(baseName + "_" + key, defValue));
		// Remember the original descriptive summary and if we have a value, show that instead
		originalSummaries.put(baseName + "_" + key, pref.getSummary() == null ? null : pref.getSummary().toString());
		showValueOnSummary(baseName + "_" + key);
		return pref;
	}

	@SuppressWarnings("deprecation")
	protected void showValueOnSummary(String prefKey) {
		Preference pref = findPreference(prefKey);
		if (sharedPrefs.contains(prefKey)
				&& pref instanceof EditTextPreference
				&& !TextUtils.isEmpty(sharedPrefs.getString(prefKey, ""))
				&& !(((EditTextPreference) pref).getEditText().getTransformationMethod() instanceof PasswordTransformationMethod)) {
			// Non-password edit preferences show the user-entered value
			pref.setSummary(sharedPrefs.getString(prefKey, ""));
			return;
		} else if (sharedPrefs.contains(prefKey) && pref instanceof ListPreference
				&& ((ListPreference) pref).getValue() != null) {
			// List preferences show the selected list value
			ListPreference listPreference = (ListPreference) pref;
			pref.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(listPreference.getValue())]);
			return;
		}
		if (originalSummaries.containsKey(prefKey))
			pref.setSummary(originalSummaries.get(prefKey));
	}

}
