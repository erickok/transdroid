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
package org.transdroid.core.gui.search;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.app.search.SearchSite;
import org.transdroid.core.app.settings.WebsearchSetting;

import java.util.List;

/**
 * Adapter that contains a list of {@link SearchSetting}s, either {@link SearchSite} or {@link WebsearchSetting}.
 * @author Eric Kok
 */
@EBean
public class SearchSitesAdapter extends BaseAdapter {

	private List<SearchSetting> sites = null;

	@RootContext
	protected Context context;

	/**
	 * Allows updating the full internal list of sites at once, replacing the old list
	 * @param sites The new list of search sites, either in-app or web search settings
	 */
	public void update(List<SearchSetting> sites) {
		this.sites = sites;
		notifyDataSetChanged();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getCount() {
		if (sites == null) {
			return 0;
		}
		return sites.size();
	}

	@Override
	public SearchSetting getItem(int position) {
		if (sites == null) {
			return null;
		}
		return sites.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SearchSiteView rssfeedView;
		if (convertView == null) {
			rssfeedView = SearchSiteView_.build(context);
		} else {
			rssfeedView = (SearchSiteView) convertView;
		}
		rssfeedView.bind(getItem(position));
		return rssfeedView;
	}

}
