package org.transdroid.core.gui.rss;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.FragmentById;
import org.transdroid.core.rssparser.Channel;

import com.actionbarsherlock.app.SherlockFragmentActivity;

@EActivity(resName = "activity_rssfeeds")
public class RssitemsActivity extends SherlockFragmentActivity {

	@Extra
	protected Channel rssfeed = null;
	
	@FragmentById(resName = "rssitems_list")
	protected RssitemsFragment fragmentItems;

	@AfterViews
	protected void init() {
		// Get the intent extras and show them to the already loaded fragment
		fragmentItems.update(rssfeed);
	}
	
}
