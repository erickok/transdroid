/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
package org.transdroid.widget;

import org.transdroid.util.TLog;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class WidgetMedium extends AppWidgetProvider {

	private static final String LOG_NAME = "Medium widget";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		for (int id : appWidgetIds) {
			TLog.d(LOG_NAME, "Update widget " + id + "?");
			if (PreferenceManager.getDefaultSharedPreferences(context).contains(org.transdroid.preferences.Preferences.KEY_WIDGET_DAEMON + id)) {
				WidgetService.scheduleUpdates(context, id);
			}
    	}
    	
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {

		for (int id : appWidgetIds) {
        	WidgetService.cancelUpdates(context, id);
    	}
		
	}
	
	// NOTE: Android 1.5 bug workaround
	@Override 
	public void onReceive(Context context, Intent intent) { 
		final String action = intent.getAction(); 
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) { 
			final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID); 
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) { 
				this.onDeleted(context, new int[] { appWidgetId }); 
			} 
		} else { 
			super.onReceive(context, intent); 
		} 
	}
	
}
