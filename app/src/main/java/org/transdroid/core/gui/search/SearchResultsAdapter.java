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
import org.transdroid.core.app.search.SearchResult;

import java.util.List;

/**
 * Adapter that contains a list of {@link SearchResult}s.
 * @author Eric Kok
 */
@EBean
public class SearchResultsAdapter extends BaseAdapter {

	private List<SearchResult> results = null;

	@RootContext
	protected Context context;

	/**
	 * Allows updating the search results, replacing the old data
	 * @param results The new list of search results
	 */
	public void update(List<SearchResult> results) {
		this.results = results;
		notifyDataSetChanged();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public int getCount() {
		if (results == null) {
			return 0;
		}
		return results.size();
	}

	@Override
	public SearchResult getItem(int position) {
		if (results == null) {
			return null;
		}
		return results.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SearchResultView rssitemView;
		if (convertView == null) {
			rssitemView = SearchResultView_.build(context);
		} else {
			rssitemView = (SearchResultView) convertView;
		}
		rssitemView.bind(getItem(position));
		return rssitemView;
	}

}
