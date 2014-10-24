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
package org.transdroid.core.seedbox;

import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.gui.settings.ServerPreference;

import android.content.Context;
import android.preference.Preference;

/**
 * Represents a {@link ServerSetting} in a preferences screen, as constructed for a specific {@link SeedboxProvider}.
 * @author Eric Kok
 */
public class SeedboxPreference extends ServerPreference {

	private SeedboxProvider provider = null;
	private OnSeedboxClickedListener onSeedboxClickedListener = null;
	private int onSeedboxClickedListenerOffset = 0;

	public SeedboxPreference(Context context) {
		super(context);
		setOnPreferenceClickListener(onSeedboxPreferenceClicked);
	}

	/**
	 * Set the seedbox provider that this server settings object is constructed for.
	 * @param provider The seedbox provider type
	 * @return Itself, for method chaining
	 */
	public SeedboxPreference setProvider(SeedboxProvider provider) {
		this.provider = provider;
		setSummary(provider.getSettings().getName());
		return this;
	}

	/**
	 * Set the server settings object that is bound to this preference item. This seedbox=specific implementation does
	 * not show the human readable server connection string, but the seedbox provider name.
	 * @param serverSetting The server settings
	 * @return Itself, for method chaining
	 */
	public SeedboxPreference setServerSetting(ServerSetting serverSetting) {
		super.setServerSetting(serverSetting);
		if (this.provider != null)
			setSummary(provider.getSettings().getName());
		return this;
	}

	/**
	 * Returns the seedbox provider for which this server is constructed.
	 * @return The seedbox provider type of this server
	 */
	public SeedboxProvider getSeedboxProvider() {
		return provider;
	}

	private OnPreferenceClickListener onSeedboxPreferenceClicked = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (onSeedboxClickedListener != null)
				onSeedboxClickedListener.onSeedboxClicked(serverSetting, provider, onSeedboxClickedListenerOffset);
			return true;
		}
	};

	/**
	 * Set a listener that will be notified of click events on this preference
	 * @param onSeedboxClickedListener The click listener to register
	 * @return Itself, for method chaining
	 */
	public ServerPreference setOnSeedboxClickedListener(OnSeedboxClickedListener onSeedboxClickedListener,
			int seedboxOffset) {
		this.onSeedboxClickedListener = onSeedboxClickedListener;
		this.onSeedboxClickedListenerOffset = seedboxOffset;
		return this;
	}

	public interface OnSeedboxClickedListener {
		public void onSeedboxClicked(ServerSetting serverSetting, SeedboxProvider provider,
				int onSeedboxClickedListenerOffset);
	}

}
