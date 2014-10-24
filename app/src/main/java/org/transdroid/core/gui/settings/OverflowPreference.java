package org.transdroid.core.gui.settings;

import org.transdroid.R;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

/**
 * A {@link Preference} item that shows an extra overflow button at the right side of the screen. The action attached to
 * this button is set using {@link #setOnOverflowClickedListener(OnOverflowClicked)}. Normal clicks on this preference
 * are handled in the standard way.
 * @author Eric Kok
 */
public class OverflowPreference extends Preference {

	private OnPreferenceClickListener onPreferenceClickListener = null;
	private OnOverflowClicked onOverflowClickedListener = null;
	private ImageButton overflowButton = null;

	public OverflowPreference(Context context) {
		super(context);
		init();
	}

	public OverflowPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public OverflowPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View layout = super.onCreateView(parent);
		// Since the Preference layout is now created, we can attach the proper click listeners
		layout.setClickable(true);
		layout.setFocusable(true);
		// When setting the background drawable the padding on this Preference layout disappears, so add it again
		int bottom = layout.getPaddingBottom();
	    int top = layout.getPaddingTop();
	    int right = layout.getPaddingRight();
	    int left = layout.getPaddingLeft();
	    layout.setBackgroundResource(R.drawable.selectable_background_holo_light);
	    layout.setPadding(left, top, right, bottom);
		layout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onPreferenceClickListener != null)
					onPreferenceClickListener.onPreferenceClick(OverflowPreference.this);
			}
		});
		overflowButton  = (ImageButton) layout.findViewById(R.id.overflow_button);
		overflowButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onOverflowClickedListener != null)
					onOverflowClickedListener.onOverflowClicked(v);
			}
		});
		return layout;
	}

	public void init() {
		// Load an overflow-style image button as custom widget in the right of this Preference layout
		setWidgetLayoutResource(R.layout.pref_withoverflow);
	}

	/**
	 * Hides the overflow button (on the right side of the Preference) from the UI.
	 */
	public void hideOverflowButton() {
		if (overflowButton != null) {
			overflowButton.setVisibility(View.GONE);
		}
	}

	/**
	 * Shows (after hiding it) the overflow button (on the right side of the Preference) in the UI.
	 */
	public void showOverflowButton() {
		if (overflowButton != null) {
			overflowButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void setOnPreferenceClickListener(OnPreferenceClickListener onPreferenceClickListener) {
		// Instead of the build-in list item click behaviour, we have to manually assign the click listener to this
		// Preference item, as we stole the focus behaviour when we added a Button to the Preference layout
		this.onPreferenceClickListener = onPreferenceClickListener;
	}
	
	/**
	 * Registers the listener for clicks on the overflow button contained in this preference.
	 * @param onOverflowClickedListener The overflow button click listener
	 */
	public void setOnOverflowClickedListener(OnOverflowClicked onOverflowClickedListener) {
		this.onOverflowClickedListener = onOverflowClickedListener;
	}

	/**
	 * An interface to be implemented by any activity (or otherwise) that wants to handle events when the contained
	 * overflow button is clicked.
	 */
	public interface OnOverflowClicked {
		public void onOverflowClicked(View overflowButton);
	}

}
