package org.transdroid.core.gui.settings;

import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;

public class InterceptableEditTextPreference extends EditTextPreference {

	private OnPreferenceClickListener overrideClickListener = null;

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public InterceptableEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public InterceptableEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public InterceptableEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public InterceptableEditTextPreference(Context context) {
		super(context);
	}

	@Override
	public OnPreferenceClickListener getOnPreferenceClickListener() {
		return overrideClickListener;
	}

	@Override
	public void setOnPreferenceClickListener(OnPreferenceClickListener onPreferenceClickListener) {
		this.overrideClickListener = onPreferenceClickListener;
	}

	@Override
	protected void onClick() {
		if (overrideClickListener == null || !overrideClickListener.onPreferenceClick(this)) {
			super.onClick();
		}
	}

}
