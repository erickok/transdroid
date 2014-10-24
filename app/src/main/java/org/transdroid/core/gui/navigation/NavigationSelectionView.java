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

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * View that displays the user-selected server and display filter inside the action bar list navigation spinner
 * @author Eric Kok
 */
@EViewGroup(resName="actionbar_navigation")
public class NavigationSelectionView extends LinearLayout {

	@ViewById
	protected TextView filterText;
	@ViewById
	protected TextView serverText;
	
	public NavigationSelectionView(Context context) {
		super(context);
	}

	/**
	 * Binds the names of the current connected server and selected filter to this navigation view.
	 * @param currentServer The name of the server currently connected to
	 * @param currentFilter The name of the filter that is currently selected
	 */
	public void bind(String currentServer, String currentFilter) {
		serverText.setText(currentServer);
		filterText.setText(currentFilter);
	}
	
}
