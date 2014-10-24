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
package org.transdroid.core.gui.search;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.app.search.SearchResult;

import android.content.Context;
import android.text.format.DateUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * View that represents a {@link SearchResult} object from an in-app search
 * @author Eric Kok
 */
@EViewGroup(resName = "list_item_searchresult")
public class SearchResultView extends RelativeLayout {

	// Views
	@ViewById
	protected TextView nameText, seedersText, leechersText, sizeText, dateText;

	public SearchResultView(Context context) {
		super(context);
	}

	public void bind(SearchResult result) {

		nameText.setText(result.getName());
		sizeText.setText(result.getSize());
		dateText.setText(result.getAddedOn() == null ? "" : DateUtils.getRelativeDateTimeString(getContext(), result
				.getAddedOn().getTime(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS,
				DateUtils.FORMAT_ABBREV_MONTH));
		seedersText.setText(getContext().getString(R.string.search_seeders, result.getSeeders()));
		leechersText.setText(getContext().getString(R.string.search_leechers, result.getLeechers()));

	}

}
