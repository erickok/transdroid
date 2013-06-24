package org.transdroid.core.gui.rss;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.SherlockListView;

/**
 * Fragment lists the RSS feeds the user wants to monitor and, if room, the list of items in a feed in a right pane.
 * @author Eric Kok
 */
@EFragment(resName = "fragment_rssfeeds")
@OptionsMenu(resName = "fragment_rssfeeds")
public class RssfeedsFragment extends SherlockFragment {

	// Views
	@ViewById(resName = "rssfeeds_list")
	protected SherlockListView feedsList;
	@Bean
	protected RssfeedsAdapter rssfeedsAdapter;
	@ViewById
	protected TextView nosettingsText;

	@AfterViews
	protected void init() {
		feedsList.setAdapter(rssfeedsAdapter);
		feedsList.setOnItemClickListener(onRssfeedSelected);
	}

	public void update(List<RssfeedLoader> loaders) {
		rssfeedsAdapter.update(loaders);
	}
	
	private OnItemClickListener onRssfeedSelected = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			((RssfeedsActivity)getActivity()).openRssfeed(rssfeedsAdapter.getItem(position));
		}
	};

	/**
	 * Notifies the contained list of RSS feeds that the underlying data has been changed.
	 */
	public void notifyDataSetChanged() {
		rssfeedsAdapter.notifyDataSetChanged();
	}
	
}
