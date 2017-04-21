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
package org.transdroid.core.gui.remoterss;

import android.content.Context;
import android.text.format.DateFormat;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.gui.remoterss.data.RemoteRssItem;

/**
 * View that represents some {@link RemoteRssItem} object.
 * @author Twig
 */
@EViewGroup(R.layout.list_item_remoterssitem)
public class RemoteRssItemView extends LinearLayout {
	// Views
	@ViewById
	protected TextView nameText, dateText, labelText;

	public RemoteRssItemView(Context context) {
		super(context);
	}

	public void bind(RemoteRssItem item) {
		labelText.setText(item.getSourceName());
		nameText.setText(item.getName());
		dateText.setText(
			DateFormat.getDateFormat(getContext()).format(item.getTimestamp()) +
			" " +
			DateFormat.getTimeFormat(getContext()).format(item.getTimestamp())
		);
	}

}
