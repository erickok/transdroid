package org.transdroid.core.gui.navigation;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * View that displays the user-selected server and display filter inside the action bar list navigation spinner
 * @author Eric Kok
 */
@EViewGroup(resName="list_item_navigation")
public class NavigationSelectionView extends LinearLayout {

	@ViewById
	protected TextView filterText;
	@ViewById
	protected TextView serverText;
	
	public NavigationSelectionView(Context context) {
		super(context);
	}

	/**
	 * Binds the names of the current connected server and selected filter to this navigation view.
	 * @param currentServer The name of the server currently connected to
	 * @param currentFilter The name of the filter that is currently selected
	 */
	public void bind(String currentServer, String currentFilter) {
		serverText.setText(currentServer);
		filterText.setText(currentFilter);
	}
	
}
