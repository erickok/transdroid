package org.transdroid.core.gui.navigation;

import org.transdroid.core.gui.navigation.SelectionModificationSpinner.OnModificationActionSelectedListener;

import android.util.SparseBooleanArray;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SherlockListView.MultiChoiceModeListenerCompat;

/**
 * A helper to implement {@link ListView} selection modification behaviour with the {@link SelectionModificationSpinner}
 * by implementing the specific actions and providing a title based on the number of currently selected items. It is
 * important that the provided list was instantiated already.
 * @author Eric Kok
 */
public class SelectionManagerMode implements MultiChoiceModeListenerCompat, OnModificationActionSelectedListener {

	private ListView managedList;
	private int titleTemplateResource;
	private Class<?> onlyCheckClass = null;

	/**
	 * Instantiates the helper by binding it to a specific {@link ListView} and providing the text resource to display
	 * as title in the spinner.
	 * @param managedList The list to manage the selection for and execute selection action to
	 * @param titleTemplateResource The string resource id to show as the spinners title; the number of selected items
	 *            will be supplied as numeric formatting argument
	 */
	public SelectionManagerMode(ListView managedList, int titleTemplateResource) {
		this.managedList = managedList;
		this.titleTemplateResource = titleTemplateResource;
	}

	/**
	 * Set the class type of items that are allowed to be checked in the {@link ListView}. Defaults to null, which means
	 * every list view row can be checked.
	 * @param onlyCheckClass The {@link Class} instance to use to check list item types against
	 */
	public void setOnlyCheckClass(Class<?> onlyCheckClass) {
		this.onlyCheckClass = onlyCheckClass;
	}
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		// Allow modification of selection through a spinner
		SelectionModificationSpinner selectionSpinner = new SelectionModificationSpinner(managedList.getContext());
		selectionSpinner.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		selectionSpinner.setOnModificationActionSelectedListener(this);
		mode.setCustomView(selectionSpinner);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		int checkedCount = 0;
		for (int i = 0; i < managedList.getCheckedItemPositions().size(); i++) {
			if (managedList.getCheckedItemPositions().valueAt(i)
					&& (onlyCheckClass == null || onlyCheckClass.isInstance(managedList.getItemAtPosition(managedList
							.getCheckedItemPositions().keyAt(i)))))
				checkedCount++;
		}
		((SelectionModificationSpinner) mode.getCustomView()).updateTitle(managedList.getContext().getResources()
				.getQuantityString(titleTemplateResource, checkedCount, checkedCount));
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		return false;
	}

	/**
	 * Implements the {@link SelectionModificationSpinner}'s invert selection command by flipping the checked status for
	 * each (enabled) items in the {@link ListView}.
	 */
	@Override
	public void invertSelection() {
		SparseBooleanArray checked = managedList.getCheckedItemPositions();
		for (int i = 0; i < managedList.getAdapter().getCount(); i++) {
			if (managedList.getAdapter().isEnabled(i)
					&& (onlyCheckClass == null || onlyCheckClass.isInstance(managedList.getItemAtPosition(i))))
				managedList.setItemChecked(i, !checked.get(i, false));
		}
	}

	/**
	 * Implements the {@link SelectionModificationSpinner}'s select all command by checking each (enabled) item in the
	 * {@link ListView}.
	 */
	@Override
	public void selectionAll() {
		for (int i = 0; i < managedList.getAdapter().getCount(); i++) {
			if (managedList.getAdapter().isEnabled(i)
					&& (onlyCheckClass == null || onlyCheckClass.isInstance(managedList.getItemAtPosition(i))))
				managedList.setItemChecked(i, true);
		}
	}

}
