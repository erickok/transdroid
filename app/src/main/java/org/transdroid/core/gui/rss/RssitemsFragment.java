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

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.gui.TorrentsActivity_;
import org.transdroid.core.gui.navigation.NavigationHelper;
import org.transdroid.core.gui.navigation.SelectionManagerMode;
import org.transdroid.core.gui.search.SearchActivity_;
import org.transdroid.core.rssparser.Channel;
import org.transdroid.core.rssparser.Item;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * Fragment that lists the items in a specific RSS feed
 * @author Eric Kok
 */
@EFragment(resName = "fragment_rssitems")
public class RssitemsFragment extends Fragment {

	@InstanceState
	protected Channel rssfeed = null;
	@InstanceState
	protected boolean hasError = false;

	// Views
	@ViewById(resName = "rssitems_list")
	protected ListView rssitemsList;
	@Bean
	protected RssitemsAdapter rssitemsAdapter;
	@ViewById
	protected TextView emptyText;

	@AfterViews
	protected void init() {

		// Set up the list adapter, which allows multi-select
		rssitemsList.setAdapter(rssitemsAdapter);
		rssitemsList.setMultiChoiceModeListener(onItemsSelected);
		update(rssfeed, hasError);

	}

	/**
	 * Update the shown RSS items in the list.
	 * @param channel The loaded RSS content channel object
	 * @param hasError True if there were errors in loading the channel, in which case an error text is shown; false
	 *            otherwise
	 */
	public void update(Channel channel, boolean hasError) {
		rssitemsAdapter.update(channel);
		rssitemsList.setVisibility(View.GONE);
		emptyText.setVisibility(View.VISIBLE);
		if (hasError) {
			emptyText.setText(R.string.rss_error);
			return;
		}
		if (channel == null) {
			emptyText.setText(R.string.rss_noselection);
			return;
		}
		if (channel.getItems().size() == 0) {
			emptyText.setText(R.string.rss_empty);
			return;
		}
		rssitemsList.setVisibility(View.VISIBLE);
		emptyText.setVisibility(View.INVISIBLE);
	}

	@ItemClick(resName = "rssitems_list")
	protected void onItemClicked(Item item) {
		// Don't broadcast this intent; we can safely assume this is intended for Transdroid only
		Intent i = TorrentsActivity_.intent(getActivity()).get();
		i.setData(item.getTheLinkUri());
		i.putExtra("TORRENT_TITLE", item.getTitle());
		startActivity(i);
	}

	private MultiChoiceModeListener onItemsSelected = new MultiChoiceModeListener() {

		SelectionManagerMode selectionManagerMode;

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Show contextual action bar to add items in batch mode
			mode.getMenuInflater().inflate(R.menu.fragment_rssitems_cab, menu);
			selectionManagerMode = new SelectionManagerMode(rssitemsList, R.plurals.rss_itemsselected);
			selectionManagerMode.onCreateActionMode(mode, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return selectionManagerMode.onPrepareActionMode(mode, menu);
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

			// Get checked torrents
			List<Item> checked = new ArrayList<Item>();
			for (int i = 0; i < rssitemsList.getCheckedItemPositions().size(); i++) {
				if (rssitemsList.getCheckedItemPositions().valueAt(i))
					checked.add(rssitemsAdapter.getItem(rssitemsList.getCheckedItemPositions().keyAt(i)));
			}

			int itemId = item.getItemId();
			if (itemId == R.id.action_addall) {
				
				// Start an Intent that adds multiple items at once, by supplying the urls and titles as string array
				// extras and setting the Intent action to ADD_MULTIPLE
				Intent intent = new Intent("org.transdroid.ADD_MULTIPLE");
				String[] urls = new String[checked.size()];
				String[] titles = new String[checked.size()];
				for (int i = 0; i < checked.size(); i++) {
					urls[i] = checked.get(i).getTheLink();
					titles[i] = checked.get(i).getTitle();
				}
				intent.putExtra("TORRENT_URLS", urls);
				intent.putExtra("TORRENT_TITLES", titles);
				startActivity(intent);
				mode.finish();
				return true;

			} else if (itemId == R.id.action_copytoclipboard) {

				StringBuilder names = new StringBuilder();
				for (int f = 0; f < checked.size(); f++) {
					if (f != 0)
						names.append("\n");
					names.append(checked.get(f).getTitle());
				}
				ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(
						Context.CLIPBOARD_SERVICE);
				clipboardManager.setPrimaryClip(ClipData.newPlainText("Transdroid", names.toString()));
				mode.finish();
				return true;

			} else {
				
				// The other items only operate on one (the first) selected item
				if (checked.size() < 1)
					return false;
				final Item first = checked.get(0);
				if (itemId == R.id.action_showdetails) {
					// Show a dialog box with the RSS item description text
					new DialogFragment() {
						public Dialog onCreateDialog(Bundle savedInstanceState) {
							return new AlertDialog.Builder(getActivity()).setMessage(first.getDescription())
									.setPositiveButton(R.string.action_close, null).create();
						};
					}.show(getFragmentManager(), "RssItemDescription");
				} else if (itemId == R.id.action_openwebsite) {
					// Open the browser to show the website contained in the item's link tag
					Toast.makeText(getActivity(), getString(R.string.search_openingdetails, first.getTitle()),
							Toast.LENGTH_LONG).show();
					if (!TextUtils.isEmpty(first.getLink())) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(first.getLink())));
					} else {
						// No URL was specified in the RSS feed item link tag (or no link tag was present)
						Crouton.showText(getActivity(), R.string.error_no_link, NavigationHelper.CROUTON_ERROR_STYLE);
					}
				} else if (itemId == R.id.action_useassearch) {
					// Use the RSS item title to start a new search (mimicking the search manager style)
					Intent search = SearchActivity_.intent(getActivity()).get();
					search.setAction(Intent.ACTION_SEARCH);
					search.putExtra(SearchManager.QUERY, first.getTitle());
					startActivity(search);
				}
				mode.finish();
				return true;
				
			}
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			selectionManagerMode.onItemCheckedStateChanged(mode, position, id, checked);
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			selectionManagerMode.onDestroyActionMode(mode);
		}

	};

}
