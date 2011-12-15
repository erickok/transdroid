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

import org.transdroid.R;
import org.transdroid.rss.RssFeedSettings;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A view that shows an rss feed and its number of unread items as a list view.
 * 
 * The number of unread items is received asynchronously.
 * 
 * @author erickok
 *
 */
public class RssFeedListView extends LinearLayout {

	//private static final String LOG_NAME = "Transdroid RSS feeds";
	
	/**
	 * Constructs a view that can display an rss feed name and number of new items since last time (to use in a list)
	 * @param context The activity context
	 * @param feedSettings The rss feed to show the data for
	 * @param unreadMessages The string showing the number of unread messages (or ? when it is still loading)
	 */
	public RssFeedListView(Context context, RssFeedSettings feedSettings, String unreadMessages) {
		super(context);
		addView(inflate(context, R.layout.list_item_rssfeed, null));
		
		setData(feedSettings, unreadMessages);
	}

	/**
	 * Sets the actual texts and images to the visible widgets (fields)
	 */
	public void setData(RssFeedSettings feedSettings, String unreadMessages) {
		TextView name = (TextView)findViewById(R.id.feed_name);
		TextView count = (TextView)findViewById(R.id.feed_unreadcount);
		ProgressBar loading = (ProgressBar)findViewById(R.id.feed_loading);

		name.setText(feedSettings.getName());
		if (unreadMessages.equals("?")) {
			loading.setVisibility(VISIBLE);
			count.setVisibility(GONE);
		} else {
			loading.setVisibility(GONE);
			count.setText(unreadMessages);
			count.setVisibility(VISIBLE);			
		}
	}

}
