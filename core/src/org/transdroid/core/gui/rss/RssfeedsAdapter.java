package org.transdroid.core.gui.rss;

import java.util.List;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.rssparser.Channel;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Adapter that contains a list of {@link RssfeedSetting}s, each with associated loaded RSS feed {@link Channel}.
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
		if (loaders == null)
			return 0;
		return loaders.size();
	}

	@Override
	public RssfeedLoader getItem(int position) {
		if (loaders == null)
			return null;
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
