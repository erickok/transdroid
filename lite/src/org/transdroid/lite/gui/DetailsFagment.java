package org.transdroid.lite.gui;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.lite.R;

import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Fragment that shown detailed statistics about some torrent. These come from some already fetched {@link Torrent} 
 * object, but it also retrieves further detailed statistics.
 * 
 * @author Eric Kok
 */
@EFragment(R.layout.fragment_details)
public class DetailsFagment extends SherlockFragment {

	@FragmentArg
	@InstanceState
	protected Torrent torrent = null;
	@InstanceState
	protected TorrentDetails torrentDetails;
	
	@ViewById
	protected TextView emptyText;
	
	@AfterViews
	protected void init() {
		
		if (torrent == null) {
			// No torrent specified; show the placeholder layout only
			emptyText.setVisibility(View.VISIBLE);
		}
		
	}
	
}
