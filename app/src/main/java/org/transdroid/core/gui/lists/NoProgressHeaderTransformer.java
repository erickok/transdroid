package org.transdroid.core.gui.lists;

import org.transdroid.R;

import uk.co.senab.actionbarpulltorefresh.library.DefaultHeaderTransformer;
import android.app.Activity;
import android.view.View;

public class NoProgressHeaderTransformer extends DefaultHeaderTransformer {

	@Override
	public void onViewCreated(Activity activity, View headerView) {
		super.onViewCreated(activity, headerView);
		setProgressBarColor(activity.getResources().getColor(R.color.green));
	}
	
}
