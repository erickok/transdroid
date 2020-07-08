package org.transdroid.core.gui.settings;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatCallback;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;

public class PreferenceCompatActivity extends PreferenceActivity implements AppCompatCallback {

	private AppCompatDelegate acd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		acd = AppCompatDelegate.create(this, this);
		acd.onCreate(savedInstanceState);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		acd.onPostCreate(savedInstanceState);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		acd.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onStop() {
		super.onStop();
		acd.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		acd.onDestroy();
	}

	public ActionBar getSupportActionBar() {
		return acd.getSupportActionBar();
	}

	@Override
	public void onSupportActionModeStarted(ActionMode actionMode) {

	}

	@Override
	public void onSupportActionModeFinished(ActionMode actionMode) {

	}

	@Nullable
	@Override
	public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) {
		return acd.startSupportActionMode(callback);
	}
}
