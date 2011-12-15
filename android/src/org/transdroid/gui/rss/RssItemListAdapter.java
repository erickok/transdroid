/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
 package org.transdroid.gui.rss;

import java.util.ArrayList;
import java.util.List;

import org.ifies.android.sax.Item;
import org.transdroid.gui.util.SelectableArrayAdapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * An adapter that can be mapped to a list of rss items.
 * @author erickok
 *
 */
public class RssItemListAdapter extends SelectableArrayAdapter<Item> {

	private List<Boolean> isNew;
	private boolean enableCheckboxes;
	
	public RssItemListAdapter(Context context, OnSelectedChangedListener listener, List<Item> items, boolean enableCheckboxes, String lastNew) {
		super(context, items, listener);
		this.enableCheckboxes = enableCheckboxes;

		// The 'last new' item url used to see which items are new since the last viewing of this feed
		// For each item in the items list, there is one boolean in the isNew list indicating if it is new
		boolean stillNew = true;
		isNew = new ArrayList<Boolean>();
		for (Item item : items) {
			if (lastNew == null || item.getTheLink() == null || item.getTheLink().equals(lastNew)) {
				stillNew = false;
			}
			isNew.add(new Boolean(stillNew));
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup paret, boolean selected) {
		// TODO: Try to reuse the view to improve scrolling performance
		return new RssItemListView(getContext(), this, getItem(position), isNew.get(position), enableCheckboxes, selected);
	}

}
