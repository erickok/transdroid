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
package org.transdroid.core.gui.lists;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class SimpleListItemAdapter extends BaseAdapter {

	private final Context context;
	private List<? extends SimpleListItem> items;
	private int autoLinkMask = 0;

	public SimpleListItemAdapter(Context context, List<? extends SimpleListItem> items) {
		this.context = context;
		this.items = items;
	}

	/**
	 * Allows updating of the full data list underlying this adapter, replacing all items
	 * @param newItems The new list of simple list items to display
	 */
	public void update(List<? extends SimpleListItem> newItems) {
		this.items = newItems;
		notifyDataSetChanged();
	}

	public void setAutoLinkMask(int autoLinkMask) {
		this.autoLinkMask = autoLinkMask; 
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
		SimpleListItemView filterItemView;
		if (convertView == null || !(convertView instanceof SimpleListItemView)) {
			filterItemView = SimpleListItemView_.build(context);
		} else {
			filterItemView = (SimpleListItemView) convertView;
		}
		filterItemView.bind(getItem(position), autoLinkMask);
		return filterItemView;
	}

	/**
	 * Represents a very simple list item that only contains a single string to show in the list. Use wrapStringsList to
	 * wrap an existing list of strings into a list of {@link SimpleListItem}s.
	 * @author Eric Kok
	 */
	public static class SimpleStringItem implements SimpleListItem {

		/**
		 * Wraps a simple string of strings into a list of SimpleStringItem to add as data to a
		 * {@link SimpleListItemAdapter}
		 * @param strings A list of string
		 * @return A list of SimpleStringItem objects representing the input strings
		 */
		public static List<SimpleStringItem> wrapStringsList(List<String> strings) {
			ArrayList<SimpleStringItem> errors = new ArrayList<SimpleStringItem>();
			if (strings != null) {
				for (String string : strings) {
					errors.add(new SimpleStringItem(string));
				}
			}
			return errors;
		}

		private final String string;

		public SimpleStringItem(String string) {
			this.string = string;
		}

		@Override
		public String getName() {
			return this.string;
		}

	}

}
