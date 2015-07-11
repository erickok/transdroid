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
package org.transdroid.core.gui.rss;

import android.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.gui.settings.MainSettingsActivity_;

import java.util.List;

/**
 * Fragment lists the RSS feeds the user wants to monitor and, if room, the list of items in a feed in a right pane.
 * @author Eric Kok
 */
@EFragment(R.layout.fragment_rssfeeds)
@OptionsMenu(R.menu.fragment_rssfeeds)
public class RssfeedsFragment extends Fragment {

	// Views
	@ViewById(R.id.rssfeeds_list)
	protected ListView feedsList;
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
		feedsList.setVisibility(hasSettings ? View.VISIBLE : View.GONE);
		nosettingsText.setVisibility(hasSettings ? View.GONE : View.VISIBLE);
		getActivity().invalidateOptionsMenu();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		boolean hasFeeds = rssfeedsAdapter != null && rssfeedsAdapter.getCount() > 0;
		menu.findItem(R.id.action_refresh).setVisible(hasFeeds);
		menu.findItem(R.id.action_settings).setShowAsAction(!hasFeeds ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@OptionsItem(R.id.action_settings)
	protected void openSettings() {
		MainSettingsActivity_.intent(getActivity()).start();
	}

	@OptionsItem(R.id.action_refresh)
	protected void refreshScreen() {
		((RssfeedsActivity) getActivity()).refreshFeeds();
	}

	@ItemClick(R.id.rssfeeds_list)
	protected void onFeedClicked(RssfeedLoader loader) {
		((RssfeedsActivity) getActivity()).openRssfeed(loader, true);
	}

	/**
	 * Notifies the contained list of RSS feeds that the underlying data has been changed.
	 */
	public void notifyDataSetChanged() {
		rssfeedsAdapter.notifyDataSetChanged();
	}

}
