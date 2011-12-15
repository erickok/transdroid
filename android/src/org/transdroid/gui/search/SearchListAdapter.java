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
package org.transdroid.gui.search;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.transdroid.R;
import org.transdroid.gui.util.SelectableArrayAdapter.OnSelectedChangedListener;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SearchListAdapter extends ResourceCursorAdapter {

	private final LayoutInflater li;

	private HashSet<String> selectedUrls;
	private OnSelectedChangedListener listener;
	
	public SearchListAdapter(Context context, Cursor c, OnSelectedChangedListener listener) {
		super(context, R.layout.list_item_search, c);
		li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.listener = listener;
		this.selectedUrls = new HashSet<String>();
	}

	@Override
	public View newView(Context context, Cursor cur, ViewGroup parent) {
		return li.inflate(R.layout.list_item_search, parent, false);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		// Set the checkbox that allow picking of multiple results at once
        final CheckBox check = (CheckBox)view.findViewById(R.id.result_check);
        // Store this row's URL in the check box view's tag
        String tag = cursor.getString(TorrentSearchTask.CURSOR_SEARCH_TORRENTURL);
        check.setTag(tag);
        check.setChecked(selectedUrls.contains(tag));
        check.setOnCheckedChangeListener(itemSelectionHandler);
        
        // Bind the data values to the text views
        TextView date = (TextView)view.findViewById(R.id.result_date);
        ((TextView)view.findViewById(R.id.result_title)).setText(cursor.getString(TorrentSearchTask.CURSOR_SEARCH_NAME));
        ((TextView)view.findViewById(R.id.result_size)).setText(cursor.getString(TorrentSearchTask.CURSOR_SEARCH_SIZE));
        ((TextView)view.findViewById(R.id.result_leechers)).setText("L: " + cursor.getInt(TorrentSearchTask.CURSOR_SEARCH_LEECHERS));
        ((TextView)view.findViewById(R.id.result_seeds)).setText("S: " + cursor.getInt(TorrentSearchTask.CURSOR_SEARCH_SEEDERS));
        long dateAdded = cursor.getLong(TorrentSearchTask.CURSOR_SEARCH_ADDED);
        if (dateAdded > 0) {
            date.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(dateAdded)));
            date.setVisibility(View.VISIBLE);
	    } else {
	            date.setText("");
	            date.setVisibility(View.GONE);
	    }
        
	}

    private final OnCheckedChangeListener itemSelectionHandler = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                itemChecked((String) buttonView.getTag(), isChecked);
        }
    };
    
	/**
	 * Is called by views to indicate an item was either selected or deselected
	 * @param url The URL for which the selected state has changed
	 * @param isChecked If the item is now checked/selected
	 */
	public void itemChecked(String url, boolean isChecked) {
		if (!isChecked && selectedUrls.contains(url)) {
			selectedUrls.remove(url);
		}
		if (isChecked && !selectedUrls.contains(url)) {
			selectedUrls.add(url);
		}
		if (listener != null) {
			listener.onSelectedResultsChanged();
		}
	}

	/**
	 * Whether an search item is currently selected
	 * @param url The search item URL, which should be present in the underlying list
	 * @return True if the search item is currently selected, false otherwise
	 */
	public boolean isItemChecked(String url) {
		return selectedUrls.contains(url);
	}

	/**
	 * Returns the list of all checked/selected items
	 * @return The list of selected items
	 */
	public Set<String> getSelectedUrls() {
		return this.selectedUrls;
	}

}
