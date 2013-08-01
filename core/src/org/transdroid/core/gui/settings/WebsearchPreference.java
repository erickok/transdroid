package org.transdroid.core.gui.settings;

import org.transdroid.core.app.settings.WebsearchSetting;

import android.content.Context;
import android.preference.Preference;

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
		public void onWebsearchClicked(WebsearchSetting serverSetting);
	}
	
}
