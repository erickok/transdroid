package org.transdroid.core.gui.rss;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.transdroid.core.app.settings.ApplicationSettings;

import com.actionbarsherlock.app.SherlockFragmentActivity;

@EActivity(resName = "activity_rssfeeds")
public class RssfeedsActivity extends SherlockFragmentActivity {

	// Settings
	@Bean
	protected ApplicationSettings applicationSettings;

	// Contained feeds and items fragments
	@FragmentById(resName = "rssfeeds_list")
	protected RssfeedsFragment fragmentFeeds;
	@FragmentById(resName = "rssitems_list")
	protected RssitemsFragment fragmentItems;

}
