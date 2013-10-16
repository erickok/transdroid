package org.transdroid.core.gui.lists;

import org.transdroid.core.R;

import android.app.Activity;
import android.view.View;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.AbsDefaultHeaderTransformer;

public class NoProgressHeaderTransformer extends AbsDefaultHeaderTransformer {

	@Override
	public void onViewCreated(Activity activity, View headerView) {
		super.onViewCreated(activity, headerView);
		setProgressBarColor(activity.getResources().getColor(R.color.green));
	}
	
}
