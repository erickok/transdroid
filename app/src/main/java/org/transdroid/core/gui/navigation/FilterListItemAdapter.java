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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.transdroid.core.gui.lists.SimpleListItem;
import org.transdroid.core.gui.lists.SimpleListItemView;

import java.util.List;

public class FilterListItemAdapter extends BaseAdapter {

	private final Context context;
	private List<? extends SimpleListItem> items;

	public FilterListItemAdapter(Context context, List<? extends SimpleListItem> items) {
		this.context = context;
		this.items = items;
	}

	/**
	 * Allows updating of the full data list underlying this adapter, replacing all items
	 * @param newItems The new list of filter items to display
	 */
	public void update(List<? extends SimpleListItem> newItems) {
		this.items = newItems;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public SimpleListItem getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		FilterListItemView filterItemView;
		if (convertView == null || !(convertView instanceof SimpleListItemView)) {
			filterItemView = FilterListItemView_.build(context);
		} else {
			filterItemView = (FilterListItemView) convertView;
		}
		filterItemView.bind(getItem(position));
		return filterItemView;
	}

}
