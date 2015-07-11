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

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.R;

/**
 * View that shows, as part of the action bar spinner, which {@link SearchSetting} is currently chosen.
 * @author Eric Kok
 */
@EViewGroup(R.layout.actionbar_searchsite)
public class SearchSettingSelectionView extends FrameLayout {

	@ViewById
	protected TextView searchsiteText;

	public SearchSettingSelectionView(Context context) {
		super(context);
	}

	public void bind(SearchSetting searchSettingItem) {
		searchsiteText.setText(searchSettingItem.getName());
	}

}
