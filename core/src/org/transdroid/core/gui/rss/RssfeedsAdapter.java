package org.transdroid.core.gui.rss;

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.app.settings.RssfeedSetting;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Adapter that contains a list of RSS feed settings.
 * @author Eric Kok
 */
@EBean
public class RssfeedsAdapter extends BaseAdapter {
	
	private List<RssfeedSetting> rssfeeds = null;
	
	@RootContext
	protected Context context;

	/**
	 * Allows updating the full internal list of feeds at once, replacing the old list
	 * @param newRssfeeds The new list of RSS feed settings objects
	 */
	public void update(List<RssfeedSetting> newRssfeeds) {
		this.rssfeeds = newRssfeeds;
		notifyDataSetChanged();
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}
	
	@Override
	public int getCount() {
		if (rssfeeds == null)
			return 0;
		return rssfeeds.size();
	}

	@Override
	public RssfeedSetting getItem(int position) {
		if (rssfeeds == null)
			return null;
		return rssfeeds.get(position);
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
