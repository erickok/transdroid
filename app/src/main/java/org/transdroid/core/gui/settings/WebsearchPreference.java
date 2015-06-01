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

import android.content.Context;
import android.preference.Preference;

import org.transdroid.core.app.settings.WebsearchSetting;

/**
 * Represents a {@link WebsearchSetting} in a preferences screen.
 * @author Eric Kok
 */
public class WebsearchPreference extends Preference {

	private static final int ORDER_START = 102;

	private WebsearchSetting websearchSetting;
	private OnWebsearchClickedListener onWebsearchClickedListener = null;

	public WebsearchPreference(Context context) {
		super(context);
		setOnPreferenceClickListener(onPreferenceClicked);
	}

	/**
	 * Set the websearch settings object that is bound to this preference item
	 * @param websearchSetting The websearch settings
	 * @return Itself, for method chaining
	 */
	public WebsearchPreference setWebsearchSetting(WebsearchSetting websearchSetting) {
		this.websearchSetting = websearchSetting;
		setTitle(websearchSetting.getName());
		setSummary(websearchSetting.getHumanReadableIdentifier());
		setOrder(ORDER_START + websearchSetting.getOrder());
		return this;
	}

	/**
	 * Set a listener that will be notified of click events on this preference
	 * @param onWebsearchClickedListener The click listener to register
	 * @return Itself, for method chaining
	 */
	public WebsearchPreference setOnWebsearchClickedListener(OnWebsearchClickedListener onWebsearchClickedListener) {
		this.onWebsearchClickedListener = onWebsearchClickedListener;
		return this;
	}

	private OnPreferenceClickListener onPreferenceClicked = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (onWebsearchClickedListener != null)
				onWebsearchClickedListener.onWebsearchClicked(websearchSetting);
			return true;
		}
	};

	public interface OnWebsearchClickedListener {
		void onWebsearchClicked(WebsearchSetting serverSetting);
	}

}
