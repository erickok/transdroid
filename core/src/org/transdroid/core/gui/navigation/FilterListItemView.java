package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.gui.lists.SimpleListItem;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * View that represents some {@link SimpleListItem} object specifically used to represent a navigation filter item.
 * @author Eric Kok
 */
@EViewGroup(resName="list_item_filter")
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
