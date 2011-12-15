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

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class WidgetUpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent broadcast) {
		int widget = broadcast.getIntExtra(WidgetService.INTENT_EXTRAS_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		Intent intent = new Intent(context, WidgetService.class);//.setAction(WidgetService.INTENT_ACTION_REFRESH);
    	intent.setData(Uri.parse("widget:" + widget)); // This is used to make the intent unique (see http://stackoverflow.com/questions/2844274)
    	intent.putExtra(WidgetService.INTENT_EXTRAS_WIDGET_ID, widget);
    	context.startService(intent);
	}

}
