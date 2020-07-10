/*
 * Copyright 2010-2018 Eric Kok et al.
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

import androidx.preference.Preference;

import org.transdroid.core.app.settings.ServerSetting;

/**
 * Represents a {@link ServerSetting} in a preferences screen.
 *
 * @author Eric Kok
 */
public class ServerPreference extends Preference {

    private static final int ORDER_START = 1;

    protected ServerSetting serverSetting;
    private OnServerClickedListener onServerClickedListener = null;
    private OnPreferenceClickListener onPreferenceClicked = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (onServerClickedListener != null)
                onServerClickedListener.onServerClicked(serverSetting);
            return true;
        }
    };

    public ServerPreference(Context context) {
        super(context);
        setOnPreferenceClickListener(onPreferenceClicked);
        setIconSpaceReserved(false);
    }

    /**
     * Set the server settings object that is bound to this preference item
     *
     * @param serverSetting The server settings
     * @return Itself, for method chaining
     */
    public ServerPreference setServerSetting(ServerSetting serverSetting) {
        this.serverSetting = serverSetting;
        setTitle(serverSetting.getName());
        setSummary(serverSetting.getHumanReadableIdentifier());
        setOrder(ORDER_START + serverSetting.getOrder());
        return this;
    }

    /**
     * Set a listener that will be notified of click events on this preference
     *
     * @param onServerClickedListener The click listener to register
     * @return Itself, for method chaining
     */
    public ServerPreference setOnServerClickedListener(OnServerClickedListener onServerClickedListener) {
        this.onServerClickedListener = onServerClickedListener;
        return this;
    }

    public interface OnServerClickedListener {
        void onServerClicked(ServerSetting serverSetting);
    }

}
