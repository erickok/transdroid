package fr.marvinlabs.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.RelativeLayout;

/**
 * Extension of a relative layout to provide a checkable behaviour
 * 
 * @author marvinlabs
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

	private boolean isChecked;
	private List<Checkable> checkableViews;

	public CheckableRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialise(attrs);
	}

	public CheckableRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialise(attrs);
	}

	public CheckableRelativeLayout(Context context, int checkableId) {
		super(context);
		initialise(null);
	}

	/*
	 * @see android.widget.Checkable#isChecked()
	 */
	public boolean isChecked() {
		return isChecked;
	}

	/*
	 * @see android.widget.Checkable#setChecked(boolean)
	 */
	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
		for (Checkable c : checkableViews) {
			c.setChecked(isChecked);
		}
	}

	/*
	 * @see android.widget.Checkable#toggle()
	 */
	public void toggle() {
		this.isChecked = !this.isChecked;
		for (Checkable c : checkableViews) {
			c.toggle();
		}
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		final int childCount = this.getChildCount();
		for (int i = 0; i < childCount; ++i) {
			findCheckableChildren(this.getChildAt(i));
		}
	}

	/**
	 * Read the custom XML attributes
	 */
	private void initialise(AttributeSet attrs) {
		this.isChecked = false;
		this.checkableViews = new ArrayList<Checkable>(5);
	}

	/**
	 * Add to our checkable list all the children of the view that implement the
	 * interface Checkable
	 */
	private void findCheckableChildren(View v) {
		if (v instanceof Checkable) {
			this.checkableViews.add((Checkable) v);
		}

		if (v instanceof ViewGroup) {
			final ViewGroup vg = (ViewGroup) v;
			final int childCount = vg.getChildCount();
			for (int i = 0; i < childCount; ++i) {
				findCheckableChildren(vg.getChildAt(i));
			}
		}
	}
}
