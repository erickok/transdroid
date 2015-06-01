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
package org.transdroid.core.gui.lists;

import android.content.Context;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.daemon.TorrentFile;

/**
 * View that represents some {@link TorrentFile} object and show the file's name, status and priority
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_torrentfile)
public class TorrentFileView extends TorrentFilePriorityLayout {

	@ViewById
	protected TextView nameText, progressText, sizesText;

	public TorrentFileView(Context context) {
		super(context, null);
	}

	public void bind(TorrentFile torrentFile) {
		nameText.setText(torrentFile.getName());
		sizesText.setText(torrentFile.getDownloadedAndTotalSizeText());
		progressText.setText(torrentFile.getProgressText());
		setPriority(torrentFile.getPriority());
	}

}
