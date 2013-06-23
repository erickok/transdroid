package org.transdroid.core.gui.rss;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.app.settings.ApplicationSettings;

import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.SherlockListView;

/**
 * Fragment lists the RSS feeds the user wants to monitor and, if room, the list of items in a feed in a right pane.
 * @author Eric Kok
 */
@EFragment(resName = "fragment_rssfeeds")
@OptionsMenu(resName = "fragment_rssfeeds")
public class RssfeedsFragment extends SherlockFragment {

	// Settings
	@Bean
	protected ApplicationSettings applicationSettings;
	@Bean
	protected RssfeedsAdapter rssfeedsAdapter;
	
	// Views
	@ViewById(resName = "rssfeeds_list")
	protected SherlockListView feedsList;
	@ViewById
	protected TextView nosettingsText;

	@AfterViews
	protected void init() {

		feedsList.setAdapter(rssfeedsAdapter);
		rssfeedsAdapter.update(applicationSettings.getRssfeedSettings());

	}

}
