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
package org.transdroid.core.widget;

import java.util.List;

import org.transdroid.R;
import org.transdroid.core.gui.lists.LocalTorrent;
import org.transdroid.daemon.Torrent;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * A list list item adapter that shows torrents as simplified, widget-style list items; the light theme is always used.
 * @author Eric Kok
 */
public class ListWidgetPreviewAdapter extends ArrayAdapter<Torrent> {

	/**
	 * Constructs the custom array adapter that shows torrents in a widget list style for preview.
	 * @param context The widget configuration activity context
	 * @param foo Ignored parameter; the light theme widget appearance is always used
	 * @param torrents The already-retrieved, non-null list of torrents to show
	 */
	public ListWidgetPreviewAdapter(Context context, int foo, List<Torrent> torrents) {
		super(context, R.layout.list_item_widget_light, torrents);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		// Get the views
		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_widget_light, parent, false);
			holder = new ViewHolder();
			holder.nameText = (TextView) convertView.findViewById(R.id.name_text);
			holder.progressText = (TextView) convertView.findViewById(R.id.progress_text);
			holder.ratioText = (TextView) convertView.findViewById(R.id.ratio_text);
			holder.statusView = convertView.findViewById(R.id.status_view);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// Bind the torrent values
		Torrent torrent = getItem(position);
		LocalTorrent local = LocalTorrent.fromTorrent(torrent);

		int statusColour;
		switch (torrent.getStatusCode()) {
		case Downloading:
			statusColour = R.color.torrent_downloading;
			break;
		case Paused:
			statusColour = R.color.torrent_paused;
			break;
		case Seeding:
			statusColour = R.color.torrent_seeding;
			break;
		case Error:
			statusColour = R.color.torrent_error;
			break;
		default: // Checking, Waiting, Queued, Unknown
			statusColour = R.color.torrent_other;
			break;
		}
		holder.nameText.setText(torrent.getName());
		holder.progressText.setText(local.getProgressSizeText(getContext().getResources(), false));
		holder.ratioText.setText(local.getProgressEtaRatioText(getContext().getResources()));
		holder.statusView.setBackgroundColor(getContext().getResources().getColor(statusColour));
		return convertView;
		
	}

	protected static class ViewHolder {
		public TextView nameText;
		public TextView progressText;
		public TextView ratioText;
		public View statusView;
	}
	
}
