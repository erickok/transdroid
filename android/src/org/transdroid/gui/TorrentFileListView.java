/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
 package org.transdroid.gui;

import org.transdroid.R;
import org.transdroid.daemon.TorrentFile;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * A view that shows a torrent file as a list item.
 * 
 * @author erickok
 *
 */
public class TorrentFileListView extends LinearLayout {

	private TorrentFile file;
	private TorrentFileListAdapter adapter;
	
	/**
	 * Constructs a view that can display a torrent file (to use in a list)
	 * @param context The activity context
	 * @param torrent The torrent file to show the data for
	 */
	public TorrentFileListView(Context context, TorrentFileListAdapter adapter, TorrentFile file, boolean initialyChecked) {
		super(context);
		this.adapter = adapter;

		addView(inflate(context, R.layout.list_item_torrentfile, null));
		setData(file, initialyChecked);
	}

	/**
	 * Sets the actual texts and images to the visible widgets (fields)
	 */
	public void setData(TorrentFile file, boolean initialyChecked) {
		this.file = file;
		final CheckBox check = (CheckBox)findViewById(R.id.check);
		check.setChecked(initialyChecked);
		check.setOnCheckedChangeListener(itemSelection);

		((TextView)findViewById(R.id.name)).setText(file.getName());
		((TextView)findViewById(R.id.sizes)).setText(file.getDownloadedAndTotalSizeText());
		((TextView)findViewById(R.id.progress)).setText(file.getProgressText());
		ImageView priority = (ImageView) findViewById(R.id.priority);
		switch (file.getPriority()) {
		case Off:
			priority.setImageResource(R.drawable.icon_priority_off);
			break;
		case Low:
			priority.setImageResource(R.drawable.icon_priority_low);
			break;
		case Normal:
			priority.setImageResource(R.drawable.icon_priority_normal);
			break;
		case High:
			priority.setImageResource(R.drawable.icon_priority_high);
			break;
		}
	}

	private OnCheckedChangeListener itemSelection = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			adapter.itemChecked(file, isChecked);
		}
	};
	
}
