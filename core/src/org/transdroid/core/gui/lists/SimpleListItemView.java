package org.transdroid.core.gui.lists;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * View that represents some {@link SimpleListItem} object and simple prints out the text (in proper style)
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_filter)
public class SimpleListItemView extends LinearLayout {

	@ViewById
	protected TextView itemText;
	
	public SimpleListItemView(Context context) {
		super(context);
	}

	public void bind(SimpleListItem filterItem) {
		itemText.setText(filterItem.getName());
	}
	
}
