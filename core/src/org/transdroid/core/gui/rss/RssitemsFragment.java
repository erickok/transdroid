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
	
	// Views
	@ViewById(resName = "rssfeeds_list")
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
		update(rssfeed);
		
	}

	/**
	 * Update the shown RSS items in the list and the known last view date. This is automatically called when the
	 * fragment is instantiated by its build, but should be manually called if it was instantiated empty.
	 * @param rssfeed The RSS feed Channel object that was retrieved
	 */
	public void update(Channel rssfeed) {
		rssitemsAdapter.update(rssfeed);
	}

	@ItemClick(resName = "rssitems_list")
	protected void onItemClicked(Item item) {
		startActivity(new Intent(Intent.ACTION_VIEW, item.getTheLinkUri()));
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
					checked.add(rssitemsAdapter.getItem(
							rssitemsList.getCheckedItemPositions().keyAt(i)));
			}

			int itemId = item.getItemId();
			if (itemId == R.id.action_addall) {
				// Start an Intent that adds multiple items at once, by supplying the urls and titles as string array
				// extras and setting the Intent action to ADD_MULTIPLE
				Intent intent = new Intent("org.transdroid.ADD_MULTIPLE");
				String[] urls = new String[checked.size()];
				String[] titles = new String[checked.size()];
				for (int i = 0; i < checked.size(); i++) {
					urls[i] = checked.get(0).getTheLink();
					titles[i] = checked.get(0).getTitle();
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
