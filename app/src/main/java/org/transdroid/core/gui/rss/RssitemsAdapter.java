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
package org.transdroid.core.gui.rss;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.rssparser.Channel;
import org.transdroid.core.rssparser.Item;

/**
 * Adapter that contains a list of {@link Item}s in an RSS feed.
 * @author Eric Kok
 */
@EBean
public class RssitemsAdapter extends BaseAdapter {

	private Channel rssfeed = null;

	@RootContext
	protected Context context;

	/**
	 * Allows updating the full RSS feed (channel and contained items), replacing the old data
	 * @param rssfeed The new RSS feed contents
	 */
	public void update(Channel rssfeed) {
		this.rssfeed = rssfeed;
		notifyDataSetChanged();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getCount() {
		if (rssfeed == null) {
			return 0;
		}
		return rssfeed.getItems().size();
	}

	@Override
	public Item getItem(int position) {
		if (rssfeed == null) {
			return null;
		}
		return rssfeed.getItems().get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RssitemView rssitemView;
		if (convertView == null) {
			rssitemView = RssitemView_.build(context);
		} else {
			rssitemView = (RssitemView) convertView;
		}
		rssitemView.bind(getItem(position));
		return rssitemView;
	}

}
