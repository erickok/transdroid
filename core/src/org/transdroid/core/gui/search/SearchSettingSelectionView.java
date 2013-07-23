package org.transdroid.core.gui.search;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * View that shows, as part of the action bar spinner, which {@link SearchSetting} is currently chosen.
 * @author Eric Kok
 */
@EViewGroup(resName = "actionbar_searchsite")
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
