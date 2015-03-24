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
import android.text.format.DateUtils;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.rssparser.Item;

/**
 * View that represents some {@link Item} object, which is a single item in some RSS feed.
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_rssitem)
public class RssitemView extends RssitemStatusLayout {

	// Views
	@ViewById
	protected TextView nameText, dateText;

	public RssitemView(Context context) {
		super(context);
	}

	public void bind(Item rssitem) {

		nameText.setText(rssitem.getTitle());
		dateText.setText(rssitem.getPubdate() == null ? "" : DateUtils
				.getRelativeDateTimeString(getContext(), rssitem.getPubdate().getTime(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS,
						DateUtils.FORMAT_ABBREV_MONTH));
		setIsNew(rssitem.isNew());

	}

}
