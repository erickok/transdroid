package org.transdroid.lite.gui.navigation;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.lite.R;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * View that represents some {@link FilterItem} object and simple prints out the text (in proper style)
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_filter)
public class FilterItemView extends LinearLayout {

	@ViewById
	protected TextView itemText;
	
	public FilterItemView(Context context) {
		super(context);
	}

	public void bind(FilterItem filterItem) {
		itemText.setText(filterItem.getName());
	}
	
}
