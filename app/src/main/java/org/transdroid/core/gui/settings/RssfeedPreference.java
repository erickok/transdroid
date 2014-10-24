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

import org.transdroid.core.app.settings.RssfeedSetting;

import android.content.Context;
import android.preference.Preference;

/**
 * Represents a {@link RssfeedSetting} in a preferences screen.
 * @author Eric Kok
 */
public class RssfeedPreference extends Preference {

	private static final int ORDER_START = 201;

	private RssfeedSetting rssfeedSetting;
	private OnRssfeedClickedListener onRssfeedClickedListener = null;
	
	public RssfeedPreference(Context context) {
		super(context);
		setOnPreferenceClickListener(onPreferenceClicked);
	}

	/**
	 * Set the RSS feed settings object that is bound to this preference item
	 * @param rssfeedSetting The RSS feed settings
	 * @return Itself, for method chaining
	 */
	public RssfeedPreference setRssfeedSetting(RssfeedSetting rssfeedSetting) {
		this.rssfeedSetting = rssfeedSetting;
		setTitle(rssfeedSetting.getName());
		setSummary(rssfeedSetting.getHumanReadableIdentifier());
		setOrder(ORDER_START + rssfeedSetting.getOrder());
		return this;
	}
	
	/**
	 * Set a listener that will be notified of click events on this preference
	 * @param onRssfeedClickedListener The click listener to register
	 * @return Itself, for method chaining
	 */
	public RssfeedPreference setOnRssfeedClickedListener(OnRssfeedClickedListener onRssfeedClickedListener) {
		this.onRssfeedClickedListener = onRssfeedClickedListener;
		return this;
	}

	private OnPreferenceClickListener onPreferenceClicked = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (onRssfeedClickedListener != null)
				onRssfeedClickedListener.onRssfeedClicked(rssfeedSetting);
			return true;
		}
	};
	
	public interface OnRssfeedClickedListener {
		public void onRssfeedClicked(RssfeedSetting rssfeedSetting);
	}
	
}
