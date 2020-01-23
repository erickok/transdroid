/* 
 * Copyright 2010-2018 Eric Kok et al.
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
package org.transdroid.core.gui.remoterss;


import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ItemSelect;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.gui.remoterss.data.RemoteRssChannel;
import org.transdroid.core.gui.remoterss.data.RemoteRssItem;
import org.transdroid.core.gui.rss.RssfeedsActivity;
import org.transdroid.core.gui.settings.MainSettingsActivity_;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that shows a list of RSS items from the server and allows the user
 * to download remotely, without having to set up RSS feeds on the Android device.
 * @author Twig
 */
@EFragment(R.layout.fragment_remoterss)
public class RemoteRssFragment extends Fragment {
	@Bean
	protected Log log;

	// Local data
	protected ArrayList<RemoteRssItem> remoteRssItems;

	// Views
	@ViewById
	protected View detailsContainer;
	@ViewById
	protected Spinner remoterssFilter;
	@ViewById
	protected ListView torrentsList;
	@ViewById
	protected TextView remoterssStatusMessage;


	@AfterViews
	protected void init() {
		// Inject menu options in the actions toolbar
		setHasOptionsMenu(true);

		// Set up details adapter
		RemoteRssItemsAdapter adapter = new RemoteRssItemsAdapter(getActivity());
		torrentsList.setAdapter(adapter);
		torrentsList.setFastScrollEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		this.refreshScreen();
	}

	@OptionsItem(R.id.action_refresh)
	protected void refreshScreen() {
		RssfeedsActivity rssActivity = (RssfeedsActivity) getActivity();
		rssActivity.refreshRemoteFeeds();
	}

	@OptionsItem(R.id.action_settings)
	protected void openSettings() {
		MainSettingsActivity_.intent(getActivity()).start();
	}

	/**
	 * Updates the UI with a new list of RSS items.
	 */
	public void updateRemoteItems(List<RemoteRssItem> remoteItems, boolean scrollToTop) {
		RemoteRssItemsAdapter adapter = (RemoteRssItemsAdapter) torrentsList.getAdapter();

		remoteRssItems = new ArrayList<>(remoteItems);
		adapter.updateItems(remoteRssItems);

		if (scrollToTop) {
			torrentsList.smoothScrollToPosition(0);
		}
		// Show/hide a nice message if there are no items to show
		if (remoteRssItems.size() > 0) {
			remoterssStatusMessage.setVisibility(View.GONE);
		}
		else {
			remoterssStatusMessage.setVisibility(View.VISIBLE);
			remoterssStatusMessage.setText(R.string.remoterss_no_files);
		}
	}

	public void updateChannelFilters(List<RemoteRssChannel> feedLabels) {
		List<String> labels = new ArrayList<>();

		for (RemoteRssChannel feedLabel : feedLabels) {
			labels.add(feedLabel.getName());
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<>(this.getContext(), android.R.layout.simple_spinner_dropdown_item, labels);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		remoterssFilter.setAdapter(adapter);
	}

	/**
	 * When the user clicks on an item, prepare to download it.
	 */
	@ItemClick(resName = "torrents_list")
	protected void detailsListClicked(int position) {
		RemoteRssItemsAdapter adapter = (RemoteRssItemsAdapter) torrentsList.getAdapter();
		RemoteRssItem item = (RemoteRssItem) adapter.getItem(position);

		((RssfeedsActivity) getActivity()).downloadRemoteRssItem(item);
	}

	@ItemSelect(R.id.remoterss_filter)
	protected void onFeedSelected(boolean selected, int position) {
		((RssfeedsActivity) getActivity()).onFeedSelected(position);
	}
}
