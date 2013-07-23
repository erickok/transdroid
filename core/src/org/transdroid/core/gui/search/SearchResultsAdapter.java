package org.transdroid.core.gui.search;

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.app.search.SearchResult;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

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
	 * @param newRssfeeds The new list of search results
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
		if (results == null)
			return 0;
		return results.size();
	}

	@Override
	public SearchResult getItem(int position) {
		if (results == null)
			return null;
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
