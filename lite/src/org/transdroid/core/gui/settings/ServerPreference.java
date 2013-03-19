package org.transdroid.core.gui.settings;

import org.transdroid.core.app.settings.ServerSetting;

import android.content.Context;
import android.preference.Preference;

/**
 * Represents a {@link ServerSetting} in a preferences screen.
 * @author Eric Kok
 */
public class ServerPreference extends Preference {

	private static final int ORDER_START = 1;

	private ServerSetting serverSetting;
	private OnServerClickedListener onServerClickedListener = null;
	
	public ServerPreference(Context context) {
		super(context);
		setOnPreferenceClickListener(onPreferenceClicked);
	}

	/**
	 * Set the server settings object that is bound to this preference item
	 * @param serverSetting The server settings
	 * @return Itself, for method chaining
	 */
	public ServerPreference setServerSetting(ServerSetting serverSetting) {
		this.serverSetting = serverSetting;
		setTitle(serverSetting.getHumanReadableIdentifier());
		setOrder(ORDER_START + serverSetting.getOrder());
		return this;
	}
	
	/**
	 * Set a listener that will be notified of click events on this preference
	 * @param onServerClickedListener The click listener to register
	 * @return Itself, for method chaining
	 */
	public ServerPreference setOnServerClickedListener(OnServerClickedListener onServerClickedListener) {
		this.onServerClickedListener = onServerClickedListener;
		return this;
	}

	private OnPreferenceClickListener onPreferenceClicked = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (onServerClickedListener != null)
				onServerClickedListener.onServerClicked(serverSetting);
			return true;
		}
	};
	
	public interface OnServerClickedListener {
		public void onServerClicked(ServerSetting serverSetting);
	}
	
}
