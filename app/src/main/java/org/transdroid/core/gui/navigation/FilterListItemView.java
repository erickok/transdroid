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
package org.transdroid.core.gui.navigation;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;
import org.transdroid.core.gui.lists.SimpleListItem;

/**
 * View that represents some {@link SimpleListItem} object specifically used to represent a navigation filter item.
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_filter)
public class FilterListItemView extends FrameLayout {

	@ViewById
	protected TextView itemText;

	public FilterListItemView(Context context) {
		super(context);
	}

	public void bind(SimpleListItem filterItem) {
		itemText.setText(filterItem.getName());
	}

}
