package org.transdroid.core.gui.lists;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.daemon.TorrentFile;

import android.content.Context;
import android.widget.TextView;

/**
 * View that represents some {@link TorrentFile} object and show the file's name, status and priority
 * @author Eric Kok
 */
@EViewGroup(resName="list_item_torrentfile")
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
