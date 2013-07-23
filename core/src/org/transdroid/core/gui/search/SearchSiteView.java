package org.transdroid.core.gui.search;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.gui.navigation.NavigationHelper;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * View that represents some {@link RssfeedSetting} object and displays name as well as loads a favicon for the feed's
 * site and can load how many new items are available.
 * @author Eric Kok
 */
@EViewGroup(resName = "list_item_searchsite")
public class SearchSiteView extends LinearLayout {

	private static final String GETFVO_URL = "http://g.etfv.co/%1$s";

	@Bean
	protected NavigationHelper navigationHelper;

	// Views
	@ViewById
	protected ImageView faviconImage;
	@ViewById
	protected TextView nameText;

	public SearchSiteView(Context context) {
		super(context);
	}

	public void bind(SearchSetting rssfeedLoader) {

		// Show the RSS feed name and either a loading indicator or the number of new items
		nameText.setText(rssfeedLoader.getName());
		// Clear and then asynchronously load the site's favicon
		// Uses the g.etfv.co service to resolve the favicon of any URL
		faviconImage.setImageDrawable(null);
		navigationHelper.getImageCache().displayImage(String.format(GETFVO_URL, rssfeedLoader.getBaseUrl()),
				faviconImage);

	}

}
