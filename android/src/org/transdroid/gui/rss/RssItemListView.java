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

import java.util.Date;

import org.ifies.android.sax.Item;
import org.transdroid.R;

import android.content.Context;
import android.text.format.DateUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * A view that shows an RSS item (which is a link to some torrent) in a list view
 * 
 * @author erickok
 *
 */
public class RssItemListView extends LinearLayout {

	private RssItemListAdapter adapter;
	private Item item;

	/**
	 * Constructs a view that can display RSS item data (to use in a list)
	 * @param context The activity context
	 * @param isNew Whether this message was new since the last viewin of the RSS feed
	 * @param torrent The RSS item to show the data for
	 */
	public RssItemListView(Context context, RssItemListAdapter adapter, Item item, boolean isNew, boolean isCheckable, boolean initiallyChecked) {
		super(context);
		this.adapter = adapter;
		
		addView(inflate(context, R.layout.list_item_rssitem, null));
		setData(item, isNew,isCheckable,  initiallyChecked);
	}

	/**
	 * Sets the actual texts and images to the visible widgets (fields)
	 */
	public void setData(Item item, boolean isNew, boolean isCheckable, boolean initiallyChecked) {
		this.item = item;

		// Set the checkbox that allow picking of multiple results at once
		final CheckBox check = (CheckBox)findViewById(R.id.rssitem_check);
		if (isCheckable) {
			check.setVisibility(VISIBLE);
			check.setChecked(initiallyChecked);
			check.setOnCheckedChangeListener(itemSelection);
		} else {
			check.setVisibility(GONE);
		}

        // Bind the data values to the text views
		ImageView icon = ((ImageView)findViewById(R.id.rssitem_new));
		TextView title = ((TextView)findViewById(R.id.rssitem_title));
		TextView date = ((TextView)findViewById(R.id.rssitem_date));
		icon.setImageResource(isNew? R.drawable.icon_new: R.drawable.icon_notnew);
		title.setText(item.getTitle());
		if (item.getPubdate() != null) {
			date.setText(DateUtils.formatSameDayTime(item.getPubdate().getTime(), new Date().getTime(), java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT));
			date.setVisibility(VISIBLE);
		} else {
			date.setText("");
			date.setVisibility(GONE);
		}
	}

	private OnCheckedChangeListener itemSelection = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			adapter.itemChecked(item, isChecked);
		}
	};
	
}
