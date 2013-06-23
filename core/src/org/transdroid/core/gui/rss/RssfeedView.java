package org.transdroid.core.gui.rss;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.gui.navigation.NavigationHelper;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * View that represents some {@link RssfeedSetting} object and displays name as well as loads a favicon for the feed's
 * site and can load how many new items are available.
 * @author Eric Kok
 */
@EViewGroup(resName = "list_item_rssfeed")
public class RssfeedView extends LinearLayout {

	private static final String GETFVO_URL = "http://g.etfv.co/%1$s";

	@Bean
	protected NavigationHelper navigationHelper;
	
	// Views
	@ViewById
	protected ImageView faviconImage;
	@ViewById
	protected TextView nameText, newcountText;
	@ViewById
	protected ProgressBar loadingProgress;

	public RssfeedView(Context context) {
		super(context);
	}

	public void bind(RssfeedSetting rssfeed) {

		nameText.setText(rssfeed.getName());
		faviconImage.setImageDrawable(null);
		loadingProgress.setVisibility(View.VISIBLE);
		newcountText.setVisibility(View.VISIBLE);
		
		// Load the RSS feed site' favicon
		// Uses the g.etfv.co service to resolve the favicon of any feed URL
		navigationHelper.getImageCache().displayImage(String.format(GETFVO_URL, rssfeed), faviconImage);
		
		// Refresh the number of new items in this feed
		refreshNewCount();
		
	}

	@Background
	protected void refreshNewCount() {
		// TODO: Implement
	}

}
