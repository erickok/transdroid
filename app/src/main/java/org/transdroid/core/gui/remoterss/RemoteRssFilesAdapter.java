package org.transdroid.core.gui.remoterss;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.transdroid.core.gui.remoterss.data.RemoteRssItem;

import java.util.ArrayList;
import java.util.List;

public class RemoteRssFilesAdapter extends BaseAdapter {
	protected Context context;
	protected List<RemoteRssItem> items;

	public RemoteRssFilesAdapter(Context context) {
		this.context = context;
		items = new ArrayList<>();
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RemoteRssItemView itemView;

		if (convertView == null) {
			itemView = RemoteRssItemView_.build(context);
		}
		else {
			itemView = (RemoteRssItemView) convertView;
		}

		itemView.bind((RemoteRssItem) getItem(position));

		return itemView;
	}

	public void updateFiles(List<RemoteRssItem> torrentFiles) {
		items = torrentFiles;
		notifyDataSetChanged();
	}
}
