package org.transdroid.core.gui.rss;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.rssparser.Channel;
import org.transdroid.core.rssparser.Item;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

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
	 * @param newRssfeeds The new RSS feed contents
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
		if (rssfeed == null)
			return 0;
		return rssfeed.getItems().size();
	}

	@Override
	public Item getItem(int position) {
		if (rssfeed == null)
			return null;
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
