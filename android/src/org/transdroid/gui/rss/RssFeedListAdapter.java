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
import java.util.Collections;
import java.util.List;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.R;
import org.transdroid.gui.util.ArrayAdapter;
import org.transdroid.rss.RssFeedSettings;
import org.transdroid.util.TLog;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * An adapter that can be mapped to a list of rss feeds.
 * @author erickok
 *
 */
public class RssFeedListAdapter extends ArrayAdapter<RssFeedSettings> {

	private static final String LOG_NAME = "RSS feeds";
	
	private List<String> unreadMessages;
	
	public RssFeedListAdapter(Context context, List<RssFeedSettings> feeds) {
		super(context, feeds);
		
		// Maintain a list where, for each of the feeds, the number of unread messages is kept
		// Initially this will show '?' (loading) and an asynchronous process will fill these
		this.unreadMessages = new ArrayList<String>();
		for (int i = 0; i < feeds.size(); i++) {
			unreadMessages.add("?");
			retrieveUnreadPostCount(i);
		}
	}
	
	public View getView(int position, View convertView, ViewGroup paret) {
		if (convertView == null) {
			// Create a new view
			return new RssFeedListView(getContext(), getItem(position), unreadMessages.get(position));
		} else {
			// Reuse view
			RssFeedListView setView = (RssFeedListView) convertView;
			setView.setData(getItem(position), unreadMessages.get(position));
			return setView;
		}
	}

	private void retrieveUnreadPostCount(final int position) {
		
		final RssFeedSettings settings = getItem(position);
		TLog.d(LOG_NAME, "Retreiving unread item count for RSS feed " + settings.getName());
		
		final Handler retrieveHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				   // Error?
				   if (msg.what == -1) {
					   // .obj contains the exception object
					    Toast.makeText(getContext(), settings.getName() + ": " + ((Exception)msg.obj).getMessage(), Toast.LENGTH_LONG).show();
					    unreadMessages.set(position, "?");
						RssFeedListAdapter.this.notifyDataSetChanged();
						return;
				   }
				   
				   // Number of unread messages is on the message .obj and the settings object key as integer in .what
				   if (Integer.parseInt(settings.getKey()) == msg.what) {
					   unreadMessages.set(position, ((Integer)msg.obj).toString());
					   RssFeedListAdapter.this.notifyDataSetChanged();
				   }
				   
			}
		};
		
		// Asynchronous getting of the number of unread messages
		new Thread() {
			@Override
			public void run() {
				try {

					// Load RSS items
					RssParser parser = new RssParser(settings.getUrl());
					parser.parse();
					if (parser.getChannel() == null) {
						throw new Exception(getContext().getResources().getString(R.string.error_norssfeed));
					}
					
					// Count items until that last known read item is found again
					// Note that the item URL is used as unique identifier
					int unread = 0;
					List<Item> items = parser.getChannel().getItems();
					Collections.sort(items, Collections.reverseOrder());
					for (Item item : items) {
						if (settings.getLastNew() == null || item == null || item.getTheLink() == null || item.getTheLink().equals(settings.getLastNew())) {
							break;
						}
						unread++;
					}
					
					// Return the found number of unread messages
					TLog.d(LOG_NAME, "RSS feed " + settings.getName() + " has " + unread + " new messages.");
					Message msg = Message.obtain();
					msg.what = Integer.parseInt(settings.getKey());
					msg.obj = unread;
					retrieveHandler.sendMessage(msg);
					
				} catch (Exception e) {
					
					// Return the error to the callback
					TLog.d(LOG_NAME, e.toString());
					Message msg = Message.obtain();
					msg.what = -1;
					msg.obj = e;
					retrieveHandler.sendMessage(msg);
				}
				
			}
		}.start();

	}
}
