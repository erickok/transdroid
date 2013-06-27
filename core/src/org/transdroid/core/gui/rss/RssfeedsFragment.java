package org.transdroid.core.gui.rss;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.gui.settings.MainSettingsActivity_;

import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
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
	}

	public void update(List<RssfeedLoader> loaders) {
		rssfeedsAdapter.update(loaders);
		boolean hasSettings = !(loaders == null || loaders.size() == 0);
		feedsList.setVisibility(hasSettings ? View.VISIBLE: View.GONE);
		nosettingsText.setVisibility(hasSettings ? View.GONE: View.VISIBLE);
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.action_settings).setShowAsAction(
				rssfeedsAdapter.getCount() == 0 ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@OptionsItem(resName = "action_settings")
	protected void openSettings() {
		MainSettingsActivity_.intent(getActivity()).start();
	}

	@OptionsItem(resName = "action_refresh")
	protected void refreshScreen() {
		((RssfeedsActivity)getActivity()).refreshFeeds();
	}

	@ItemClick(resName = "rssfeeds_list")
	protected void onFeedClicked(RssfeedLoader loader) {
		((RssfeedsActivity)getActivity()).openRssfeed(loader, true);
	}
	
	/**
	 * Notifies the contained list of RSS feeds that the underlying data has been changed.
	 */
	public void notifyDataSetChanged() {
		rssfeedsAdapter.notifyDataSetChanged();
	}
	
}
