package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * A list item that shows a sub header or separator (in underlined Holo style).
 * @author Eric Kok
 */
@EViewGroup(resName="list_item_separator")
public class FilterSeparatorView extends FrameLayout {

	protected String text;
	
	@ViewById
	protected TextView separatorText;
	
	public FilterSeparatorView(Context context) {
		super(context);
	}

	/**
	 * Sets the text that will be shown in this separator (sub header)
	 * @param text The new text to show
	 * @return Itself, for convenience of method chaining
	 */
	public FilterSeparatorView setText(String text) {
		separatorText.setText(text);
		setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.WRAP_CONTENT,
				AbsListView.LayoutParams.WRAP_CONTENT));
		return this;
	}
	
}
