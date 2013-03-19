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
	
	private NavigationFilterManager navigationFilterManager;
	
	public NavigationSelectionView(Context context) {
		super(context);
	}

	/**
	 * Stores which screen, or manager, handles navigation selection and display
	 * @param manager The navigation manager, which knows about the currently selected filter and server
	 * @return Itself, for method chaining
	 */
	public NavigationSelectionView setNavigationFilterManager(NavigationFilterManager manager) {
		this.navigationFilterManager = manager;
		return this;
	}
	
	public void bind() {
		filterText.setText(navigationFilterManager.getActiveFilterText());
		serverText.setText(navigationFilterManager.getActiveServerText());
	}
	
	/**
	 * Interface that the manager of navigation (selecting servers and filters) should implement
	 */
	public interface NavigationFilterManager {
		String getActiveFilterText();
		String getActiveServerText();
	}
	
}
