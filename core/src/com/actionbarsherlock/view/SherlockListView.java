package com.actionbarsherlock.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Checkable;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Provides backwards compatible multiple choice ActionMode support on Froyo+ using ActionBarSherlock.
 */
public class SherlockListView extends ListView {
	// API 11+ reference, but ok because the value will be inlined.
	public static final int CHOICE_MODE_MULTIPLE_MODAL_COMPAT = CHOICE_MODE_MULTIPLE_MODAL;

	/**
	 * Wrapper to intercept delegation of long click events, and pass to {@link #doLongPress}
	 */
	class OnItemLongClickListenerWrapper implements OnItemLongClickListener {
		private OnItemLongClickListener wrapped;

		public void setWrapped(OnItemLongClickListener listener) {
			this.wrapped = listener;
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> view, View child, int position, long id) {
			// this would be easier if AbsListView.performLongPress wasn't package
			// protected :-(
			boolean handled = doLongPress(child, position, id);
			if (!handled && wrapped != null) {
				return wrapped.onItemLongClick(view, child, position, id);
			}
			return true;
		}
	}

	/**
	 * Hijack the onLongClickListener so we can intercept delegation.
	 */
	@Override
	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		if (longClickListenerWrapper == null) {
			longClickListenerWrapper = new OnItemLongClickListenerWrapper();
		}
		longClickListenerWrapper.setWrapped(listener);
		super.setOnItemLongClickListener(longClickListenerWrapper);
	}

	/**
	 * A MultiChoiceModeListener receives events for {@link AbsListView#CHOICE_MODE_MULTIPLE_MODAL}. It acts as the
	 * {@link ActionMode.Callback} for the selection mode and also receives
	 * {@link #onItemCheckedStateChanged(ActionMode, int, long, boolean)} events when the user selects and deselects
	 * list items.
	 */
	@SuppressWarnings("javadoc")
	public interface MultiChoiceModeListenerCompat extends ActionMode.Callback {
		/**
		 * Called when an item is checked or unchecked during selection mode.
		 * @param mode The {@link ActionMode} providing the selection mode
		 * @param position Adapter position of the item that was checked or unchecked
		 * @param id Adapter ID of the item that was checked or unchecked
		 * @param checked <code>true</code> if the item is now checked, <code>false</code> if the item is now unchecked.
		 */
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked);
	}

	class MultiChoiceModeWrapper implements MultiChoiceModeListenerCompat {
		private MultiChoiceModeListenerCompat wrapped;

		public void setWrapped(MultiChoiceModeListenerCompat wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			if (wrapped == null) {
				return false;
			}
			if (wrapped.onCreateActionMode(mode, menu)) {
				// Initialize checked graphic state?
				setLongClickable(false);
				return true;
			}
			return false;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			if (wrapped == null) {
				return false;
			}
			return wrapped.onPrepareActionMode(mode, menu);
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (wrapped == null) {
				return false;
			}
			return wrapped.onActionItemClicked(mode, item);
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			if (wrapped == null) {
				return;
			}
			wrapped.onDestroyActionMode(mode);
			actionMode = null;

			// Ending selection mode means deselecting everything.
			clearChoices();
			checkedItemCount = 0;
			updateOnScreenCheckedViews();
			invalidateViews();
			setLongClickable(true);
			requestLayout();
			invalidate();
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
			if (wrapped == null) {
				return;
			}
			wrapped.onItemCheckedStateChanged(mode, position, id, checked);

			// If there are no items selected we no longer need the selection mode.
			if (checkedItemCount == 0) {
				mode.finish();
			}
		}
	}

	private com.actionbarsherlock.view.ActionMode actionMode;
	private OnItemLongClickListenerWrapper longClickListenerWrapper;
	private MultiChoiceModeWrapper choiceModeListener;
	private int choiceMode;
	private int checkedItemCount;

	public SherlockListView(Context context) {
		super(context);
		init(context);
	}

	public SherlockListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public SherlockListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	void init(Context context) {
		if (isInEditMode()) {
			// Ignore when viewing in the UI designer
			return;
		}
		if (!(context instanceof SherlockActivity || context instanceof SherlockFragmentActivity)) {
			throw new IllegalStateException(
					"This view must be hosted in a SherlockActivity or SherlockFragmentActivity");
		}
		setOnItemLongClickListener(null);
	}

	@Override
	public void setChoiceMode(int mode) {
		choiceMode = mode;
		if (actionMode != null) {
			actionMode.finish();
			actionMode = null;
		}
		if (choiceMode != CHOICE_MODE_NONE) {
			if (mode == CHOICE_MODE_MULTIPLE_MODAL_COMPAT) {
				clearChoices();
				checkedItemCount = 0;
				setLongClickable(true);
				updateOnScreenCheckedViews();
				requestLayout();
				invalidate();
				mode = CHOICE_MODE_MULTIPLE;
			}
			super.setChoiceMode(mode);
		}
	}

	@Override
	public int getChoiceMode() {
		return choiceMode;
	}

	public void setMultiChoiceModeListener(MultiChoiceModeListenerCompat listener) {
		if (choiceModeListener == null) {
			choiceModeListener = new MultiChoiceModeWrapper();
		}
		choiceModeListener.setWrapped(listener);
	}

	@Override
	public boolean performItemClick(View view, int position, long id) {
		boolean handled = false;
		boolean dispatchItemClick = true;
		boolean checkStateChanged = false;
		if (choiceMode != CHOICE_MODE_NONE) {
			handled = true;
			if (choiceMode == CHOICE_MODE_MULTIPLE
					|| (choiceMode == CHOICE_MODE_MULTIPLE_MODAL_COMPAT && actionMode != null)) {
				boolean newValue = !getCheckedItemPositions().get(position);
				setItemChecked(position, newValue);
				if (actionMode != null) {
					choiceModeListener.onItemCheckedStateChanged(actionMode, position, id, newValue);
					dispatchItemClick = false;
				}
				checkStateChanged = true;
				return false;
			} else if (choiceMode == CHOICE_MODE_SINGLE) {
				boolean newValue = !getCheckedItemPositions().get(position);
				setItemChecked(position, newValue);
				checkStateChanged = true;
			}
			if (checkStateChanged) {
				updateOnScreenCheckedViews();
			}
		}
		if (dispatchItemClick) {
			handled |= super.performItemClick(view, position, id);
		}
		return handled;
	}

	/**
	 * Perform a quick, in-place update of the checked or activated state on all visible item views. This should only be
	 * called when a valid choice mode is active.
	 * <p>
	 * (Taken verbatim from AbsListView.java)
	 */
	@TargetApi(11)
	private void updateOnScreenCheckedViews() {
		final int firstPos = getFirstVisiblePosition();
		final int count = getChildCount();
		final boolean useActivated = getContext().getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.HONEYCOMB;
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			final int position = firstPos + i;

			if (child instanceof Checkable) {
				((Checkable) child).setChecked(getCheckedItemPositions().get(position));
			} else if (useActivated) {
				child.setActivated(getCheckedItemPositions().get(position));
			}
		}
	}

	public ActionMode startActionMode(ActionMode.Callback callback) {
		if (actionMode != null) {
			return actionMode;
		}
		Context context = getContext();
		if (context instanceof SherlockActivity) {
			actionMode = ((SherlockActivity) getContext()).startActionMode(callback);
		} else if (context instanceof SherlockFragmentActivity) {
			actionMode = ((SherlockFragmentActivity) context).startActionMode(callback);
		} else {
			throw new IllegalStateException(
					"This view must be hosted in a SherlockActivity or SherlockFragmentActivity");
		}
		return actionMode;
	}

	boolean doLongPress(final View child, final int longPressPosition, final long longPressId) {
		if (choiceMode == CHOICE_MODE_MULTIPLE_MODAL_COMPAT) {
			if (actionMode == null && (actionMode = startActionMode(choiceModeListener)) != null) {
				setItemChecked(longPressPosition, true);
			}
			return true;
		}
		return false;
	}

	/**
	 * Sets the checked state of the specified position. The is only valid if the choice mode has been set to
	 * {@link #CHOICE_MODE_SINGLE} or {@link #CHOICE_MODE_MULTIPLE}.
	 * @param position The item whose checked state is to be checked
	 * @param value The new checked state for the item
	 */
	@Override
	public void setItemChecked(int position, boolean value) {
		if (choiceMode == CHOICE_MODE_NONE) {
			return;
		}
		SparseBooleanArray checkStates = getCheckedItemPositions();

		// Start selection mode if needed. We don't need to if we're unchecking
		// something.
		if (value && choiceMode == CHOICE_MODE_MULTIPLE_MODAL_COMPAT && actionMode == null) {
			actionMode = startActionMode(choiceModeListener);
		}

		if (choiceMode == CHOICE_MODE_MULTIPLE || choiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
			// boolean oldValue = checkStates.get(position);
			checkStates.put(position, value);
			if (value) {
				checkedItemCount++;
			} else {
				checkedItemCount--;
			}
			if (actionMode != null) {
				final long id = getAdapter().getItemId(position);
				choiceModeListener.onItemCheckedStateChanged(actionMode, position, id, value);
			}
		} else {
			if (value || isItemChecked(position)) {
				checkStates.clear();
			}
			// this may end up selecting the value we just cleared but this way
			// we ensure length of checkStates is 1, a fact getCheckedItemPosition
			// relies on
			if (value) {
				checkStates.put(position, true);
			}
		}
		requestLayout();
		invalidate();
	}
}
