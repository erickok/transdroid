package org.transdroid.core.gui.rss;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.gui.navigation.SelectionManagerMode;
import org.transdroid.core.rssparser.Channel;
import org.transdroid.core.rssparser.Item;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SherlockListView;
import com.actionbarsherlock.view.SherlockListView.MultiChoiceModeListenerCompat;

/**
 * Fragment that lists the items in a specific RSS feed
 * @author Eric Kok
 */
@EFragment(resName = "fragment_rssitems")
public class RssitemsFragment extends SherlockFragment {

	@InstanceState
	protected Channel rssfeed = null;
	@InstanceState
	protected boolean hasError = false;

	// Views
	@ViewById(resName = "rssitems_list")
	protected SherlockListView rssitemsList;
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
		Intent i = new Intent(Intent.ACTION_VIEW, item.getTheLinkUri());
		i.putExtra("TORRENT_TITLE", item.getTitle());
		startActivity(i);
	}

	private MultiChoiceModeListenerCompat onItemsSelected = new MultiChoiceModeListenerCompat() {

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
			} else {
				return false;
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
