package org.transdroid.core.gui.remoterss;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.transdroid.daemon.Utorrent.data.RemoteRssFile;

import java.util.ArrayList;
import java.util.List;

public class RemoteRssFilesAdapter extends BaseAdapter {
	protected Context context;
	protected List<RemoteRssFile> files;

	public RemoteRssFilesAdapter(Context context) {
		this.context = context;
		files = new ArrayList<>();
	}

	@Override
	public int getCount() {
		return files.size();
	}

	@Override
	public Object getItem(int position) {
		return files.get(position);
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

		itemView.bind((RemoteRssFile) getItem(position));

		return itemView;
	}

	public void updateFiles(List<RemoteRssFile> torrentFiles) {
		files = torrentFiles;
		notifyDataSetChanged();
	}
}
