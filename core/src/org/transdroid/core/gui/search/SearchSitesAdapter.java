package org.transdroid.core.gui.search;

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.app.search.SearchSite;
import org.transdroid.core.app.settings.WebsearchSetting;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

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
		if (sites == null)
			return 0;
		return sites.size();
	}

	@Override
	public SearchSetting getItem(int position) {
		if (sites == null)
			return null;
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
