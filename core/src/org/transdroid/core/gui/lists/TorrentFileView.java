package org.transdroid.core.gui.lists;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.daemon.TorrentFile;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import fr.marvinlabs.widget.CheckableRelativeLayout;

/**
 * View that represents some {@link TorrentFile} object and show the file's name, status and priority
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_torrentfile)
public class TorrentFileView extends CheckableRelativeLayout {

	@ViewById
	protected TextView nameText, progressText, sizesText;
	@ViewById
	protected ImageView priorityImage;
	
	public TorrentFileView(Context context) {
		super(context, null);
	}

	public void bind(TorrentFile torrentFile) {
		nameText.setText(torrentFile.getName());
		sizesText.setText(torrentFile.getDownloadedAndTotalSizeText());
		progressText.setText(torrentFile.getProgressText());
		switch (torrentFile.getPriority()) {
		case Off:
			priorityImage.setImageResource(R.drawable.ic_priority_off);
			priorityImage.setContentDescription(getResources().getString(R.string.status_priority_low));
			break;
		case Low:
			priorityImage.setImageResource(R.drawable.ic_priority_low);
			priorityImage.setContentDescription(getResources().getString(R.string.status_priority_normal));
			break;
		case Normal:
			priorityImage.setImageResource(R.drawable.ic_priority_normal);
			priorityImage.setContentDescription(getResources().getString(R.string.status_priority_normal));
			break;
		case High:
			priorityImage.setImageResource(R.drawable.ic_priority_high);
			priorityImage.setContentDescription(getResources().getString(R.string.status_priority_high));
			break;
		}
	}
	
}
