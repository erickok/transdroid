package org.transdroid.core.gui.rss;

import java.util.Date;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.ifies.android.sax.Channel;

import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.SherlockListView;

/**
 * Fragment that lists the items in a specific RSS feed
 * @author Eric Kok
 */
@EFragment(resName = "fragment_rssitems")
public class RssitemsFragment extends SherlockFragment {

	@FragmentArg
	@InstanceState
	protected Channel rssfeed;
	@FragmentArg
	@InstanceState
	protected Date lastViewedItem;
	
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
		rssitemsAdapter.setLastItemViewed(lastViewedItem);
		rssitemsAdapter.update(rssfeed);

	}

}
