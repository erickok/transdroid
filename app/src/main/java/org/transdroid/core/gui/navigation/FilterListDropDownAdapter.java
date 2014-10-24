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
package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.EBean;
import org.transdroid.daemon.IDaemonAdapter;

import android.view.View;
import android.view.ViewGroup;

/**
 * List adapter that holds filter items, that is, servers, view types and labels and is displayed as content to a 
 * Spinner instead of a ListView.
 * @author Eric Kok
 */
@EBean
public class FilterListDropDownAdapter extends FilterListAdapter {

	protected NavigationSelectionView navigationSelectionView = null;
	private String currentServer = null;
	private String currentFilter = null;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// This returns the singleton navigation spinner view
		if (navigationSelectionView == null) {
			navigationSelectionView = NavigationSelectionView_.build(context);
		}
		navigationSelectionView.bind(currentServer, currentFilter);
		return navigationSelectionView;
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// This returns the item to show in the drop down list
		return super.getView(position, convertView, parent);
	}

	public void updateCurrentFilter(NavigationFilter currentFilter) {
		this.currentFilter = currentFilter.getName();
		if (navigationSelectionView != null)
			navigationSelectionView.bind(this.currentServer, this.currentFilter);
	}

	public void updateCurrentServer(IDaemonAdapter currentConnection) {
		this.currentServer = currentConnection.getSettings().getName();
		if (navigationSelectionView != null)
			navigationSelectionView.bind(this.currentServer, this.currentFilter);
	}

	public void hideServersLabel() {
		serverSeparator.setViewVisibility(View.GONE);
		notifyDataSetInvalidated();
	}
	
}
