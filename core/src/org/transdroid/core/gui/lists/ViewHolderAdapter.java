package org.transdroid.core.gui.lists;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

/**
 * A simple wrapper adapter around a single view, typically for use in a {@link MergeAdapter}. Notably, this adapter
 * handles the proper showing or hiding of the view according to the contained view's visibility if
 * {@link #setViewVisibility(int)} is used on this adapter rather than setting the visibility of the view directly on
 * the view object. This is required since otherwise the adapter's consumer (i.e. a {@link ListView}) does not update
 * the list row accordingly. Use {@link #setViewEnabled(boolean)} to enable or disable this contained view for user
 * interaction.
 * @author Eric Kok
 */
public class ViewHolderAdapter extends BaseAdapter {

	private final View view;

	/**
	 * Instantiates this wrapper adapter with the one and only view to show. It can not be updated and view visibility
	 * should be set directly on this adapter using {@link #setViewVisibility(int)}. Use
	 * {@link #setViewEnabled(boolean)} to enable or disable this contained view for user interaction.
	 * @param view The view that will be wrapper in an adapter to show in a list view
	 */
	public ViewHolderAdapter(View view) {
		this.view = view;
	}

	/**
	 * Sets the visibility on the contained view and notifies consumers of this adapter (i.e. a {@link ListView})
	 * accordingly. Use {@link View#GONE} to hide this adapter's view altogether.
	 * @param visibility The visibility to set on the contained view
	 */
	public void setViewVisibility(int visibility) {
		view.setVisibility(visibility);
		notifyDataSetChanged();
	}

	/**
	 * Sets whether the contained view should be enabled and notifies consumers of this adapter (i.e. a {@link ListView}
	 * ) accordingly. A contained enabled view allows user interaction (clicks, focus), while a disabled view does not.
	 * @param enabled Whether the contained view should be enabled
	 */
	public void setViewEnabled(boolean enabled) {
		view.setEnabled(enabled);
		notifyDataSetChanged();
	}

	/**
	 * Returns 1 if the contained view is {@link View#VISIBLE} or {@link View#INVISIBLE}, return 0 if {@link View#GONE}.
	 */
	@Override
	public int getCount() {
		return view.getVisibility() == View.VISIBLE ? 1 : 0;
	}

	/**
	 * Always directly returns the single contained view instance.
	 */
	@Override
	public Object getItem(int position) {
		return view;
	}

	/**
	 * Always returns the position directly as item id.
	 */
	@Override
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Always directly returns the single contained view instance.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return view;
	}

	/**
	 * Always returns true, as there is only one contained item and it is never changed.
	 */
	@Override
	public boolean hasStableIds() {
		return true;
	}

	/**
	 * Returns false, as the contained view can still be enabled and disabled.
	 */
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	/**
	 * Returns true if the contained view is enabled, returns false otherwise.
	 */
	@Override
	public boolean isEnabled(int position) {
		return view.isEnabled();
	}

}
