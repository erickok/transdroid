package org.transdroid.core.gui.rss;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.rssparser.Channel;

import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.SherlockListView;

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
		rssitemsList.setAdapter(rssitemsAdapter);
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

}
