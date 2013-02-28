package org.transdroid.lite.gui.navigation;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.lite.R;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A list item that shows a sub header or separator (in underlined Holo style).
 * 
 * @author Eric Kok
 */
@EViewGroup(R.layout.list_item_separator)
public class FilterSeparatorView extends LinearLayout {

	protected String text;
	
	@ViewById
	protected TextView separatorText;
	
	public FilterSeparatorView(Context context) {
		super(context);
	}

	public void bind(FilterItem filterItem) {
		separatorText.setText(text);
	}
	
	/**
	 * Sets the text that will be shown in this separator (sub header)
	 * @param text The new text to show
	 * @return Itself, for convenience of method chaining
	 */
	public FilterSeparatorView setText(String text) {
		this.text = text;
		return this;
	}
	
}
