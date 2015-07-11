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
import org.transdroid.core.app.settings.RssfeedSetting;

import java.util.List;

/**
 * Adapter that contains a list of {@link RssfeedSetting}s, each with associated loaded RSS feed {@link org.transdroid.core.rssparser.Channel}.
 * @author Eric Kok
 */
@EBean
public class RssfeedsAdapter extends BaseAdapter {

	private List<RssfeedLoader> loaders = null;

	@RootContext
	protected Context context;

	/**
	 * Allows updating the full internal list of feed loaders at once, replacing the old list
	 * @param loaders The new list of RSS feed loader objects, which pair settings and a loaded channel
	 */
	public void update(List<RssfeedLoader> loaders) {
		this.loaders = loaders;
		notifyDataSetChanged();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getCount() {
		if (loaders == null) {
			return 0;
		}
		return loaders.size();
	}

	@Override
	public RssfeedLoader getItem(int position) {
		if (loaders == null) {
			return null;
		}
		return loaders.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RssfeedView rssfeedView;
		if (convertView == null) {
			rssfeedView = RssfeedView_.build(context);
		} else {
			rssfeedView = (RssfeedView) convertView;
		}
		rssfeedView.bind(getItem(position));
		return rssfeedView;
	}

}
