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
package org.transdroid.core.gui.search;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.navigation.FilterListItemAdapter;

import java.util.List;

/**
 * List adapter that holds search settings, that is, web searches and in-app search sites, displayed as content to a Spinner instead of a ListView.
 * @author Eric Kok
 */
public class SearchSettingsDropDownAdapter extends FilterListItemAdapter {

	private final Context context;

	public SearchSettingsDropDownAdapter(Context context, List<? extends SimpleListItem> items) {
		super(context, items);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// This returns the item to show in the action bar spinner
		SearchSettingSelectionView filterItemView;
		if (convertView == null || !(convertView instanceof SearchSettingSelectionView)) {
			filterItemView = SearchSettingSelectionView_.build(context);
		} else {
			filterItemView = (SearchSettingSelectionView) convertView;
		}
		filterItemView.bind((SearchSetting) getItem(position));
		return filterItemView;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// This returns the item to show in the drop down list
		return super.getView(position, convertView, parent);
	}

}
