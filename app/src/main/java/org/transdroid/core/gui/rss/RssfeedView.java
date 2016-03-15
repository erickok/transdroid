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

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.app.settings.RssfeedSetting;
import org.transdroid.core.gui.navigation.NavigationHelper;

/**
 * View that represents some {@link RssfeedSetting} object and displays name as well as loads a favicon for the feed's site and can load how many new
 * items are available.
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_rssfeed)
public class RssfeedView extends LinearLayout {

	private static final String GRABICON_URL = "http://icons.better-idea.org/icon?url=%1$s&size=72";

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

	public void bind(RssfeedLoader rssfeedLoader) {

		// Show the RSS feed name and either a loading indicator or the number of new items
		nameText.setText(rssfeedLoader.getSetting().getName());
		if (rssfeedLoader.hasError() || rssfeedLoader.getChannel() != null) {
			loadingProgress.setVisibility(View.GONE);
			newcountText.setVisibility(View.VISIBLE);
			newcountText.setText(rssfeedLoader.hasError() ? "?" : Integer.toString(rssfeedLoader.getNewCount()));
		} else {
			loadingProgress.setVisibility(View.VISIBLE);
			newcountText.setVisibility(View.GONE);
		}

		// Clear and then asynchronously load the RSS feed site' favicon
		// Uses the g.etfv.co service to resolve the favicon of any feed URL
		faviconImage.setImageDrawable(null);
		navigationHelper.getImageCache().displayImage(String.format(GRABICON_URL, rssfeedLoader.getSetting().getUrl()), faviconImage);

	}

}
