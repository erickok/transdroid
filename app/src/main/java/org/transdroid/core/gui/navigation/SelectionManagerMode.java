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
package org.transdroid.core.gui.navigation;

import org.transdroid.core.gui.navigation.SelectionModificationSpinner.OnModificationActionSelectedListener;
import org.transdroid.daemon.Finishable;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

/**
 * A helper to implement {@link ListView} selection modification behaviour with the {@link SelectionModificationSpinner}
 * by implementing the specific actions and providing a title based on the number of currently selected items. It is
 * important that the provided list was instantiated already.
 * @author Eric Kok
 */
public class SelectionManagerMode implements MultiChoiceModeListener, OnModificationActionSelectedListener {

	private final Context themedContext;
	private final ListView managedList;
	private final int titleTemplateResource;
	private Class<?> onlyCheckClass = null;

	/**
	 * Instantiates the helper by binding it to a specific {@link ListView} and providing the text resource to display
	 * as title in the spinner.
	 * @param themedContext The context which is associated with the correct theme to apply when inflating views, i.e. the toolbar context
	 * @param managedList The list to manage the selection for and execute selection action to
	 * @param titleTemplateResource The string resource id to show as the spinners title; the number of selected items
	 *            will be supplied as numeric formatting argument
	 */
	public SelectionManagerMode(Context themedContext, ListView managedList, int titleTemplateResource) {
		this.themedContext = themedContext;
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
		SelectionModificationSpinner selectionSpinner = new SelectionModificationSpinner(themedContext);
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
		((SelectionModificationSpinner) mode.getCustomView()).updateTitle(themedContext.getResources()
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
	public void selectAll() {
		for (int i = 0; i < managedList.getAdapter().getCount(); i++) {
			if (managedList.getAdapter().isEnabled(i)
					&& (onlyCheckClass == null || onlyCheckClass.isInstance(managedList.getItemAtPosition(i))))
				managedList.setItemChecked(i, true);
		}
	}

	/**
	 * Implements the {@link SelectionModificationSpinner}'s select finished command by checking each (enabled) item
	 * that represents something that is {@link Finishable} and indeed is finished;
	 */
	@Override
	public void selectFinished() {
		for (int i = 0; i < managedList.getAdapter().getCount(); i++) {
			if (managedList.getAdapter().isEnabled(i)
					&& (onlyCheckClass == null || onlyCheckClass.isInstance(managedList.getItemAtPosition(i)))
					&& managedList.getItemAtPosition(i) instanceof Finishable)
				managedList.setItemChecked(i, ((Finishable) managedList.getItemAtPosition(i)).isFinished());
		}
	}

}
