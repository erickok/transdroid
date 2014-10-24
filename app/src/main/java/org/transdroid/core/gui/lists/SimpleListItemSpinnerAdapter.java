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

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * A wrapper around {@link ArrayAdapter} that contains {@link SimpleListItem}s which simply show their name in the
 * Spinner. The standard Android spinner resources are used for the layout.
 * @author Eric Kok
 */
public class SimpleListItemSpinnerAdapter<T extends SimpleListItem> extends ArrayAdapter<T> {

	/**
	 * Constructs the adapter, supplying the {@link SimpleListItem}s to show in the spinner. The given resource will be
	 * ignored as the standard Android Spinner layout is used instead.
	 * @param context The UI context to inflate the layout in
	 * @param resource This is ignored; android.R.layout.simple_spinner_item is always used instead
	 * @param objects The items to show in the spinner, which can simply display some name
	 */
	public SimpleListItemSpinnerAdapter(Context context, int resource, List<T> objects) {
		super(context, android.R.layout.simple_spinner_item, objects);
		setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// This relies on the ArrayAdapter implementation and the used standard xml layouts that simply return a
		// TextView; this can then be filled with the SimpleListItem's name instead of the standard toString()
		// implementation
		TextView text = (TextView) super.getView(position, convertView, parent);
		text.setText(getItem(position).getName());
		return text;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// This relies on the ArrayAdapter implementation and the used standard xml layouts that simply return a
		// TextView; this can then be filled with the SimpleListItem's name instead of the standard toString()
		// implementation
		TextView text = (TextView) super.getDropDownView(position, convertView, parent);
		text.setText(getItem(position).getName());
		return text;
	}
	
}
