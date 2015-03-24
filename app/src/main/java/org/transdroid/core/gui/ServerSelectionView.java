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
package org.transdroid.core.gui;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.gui.navigation.NavigationFilter;
import org.transdroid.daemon.IDaemonAdapter;

@EViewGroup(R.layout.actionbar_serverselection)
public class ServerSelectionView extends RelativeLayout {

	@ViewById
	protected TextView filterText, serverText;

	public ServerSelectionView(Context context) {
		super(context);
	}

	public ServerSelectionView(TorrentsActivity activity) {
		super(activity.torrentsToolbar.getContext());
	}

	/**
	 * Updates the name of the current connected server.
	 * @param currentServer The server currently connected to
	 */
	public void updateCurrentServer(IDaemonAdapter currentServer) {
		serverText.setText(currentServer.getSettings().getName());
	}

	/**
	 * Updates the name of the selected filter.
	 * @param currentFilter The filter that is currently selected
	 */
	public void updateCurrentFilter(NavigationFilter currentFilter) {
		filterText.setText(currentFilter.getName());
	}

}
