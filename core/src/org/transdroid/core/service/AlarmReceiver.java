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
package org.transdroid.core.service;

import org.androidannotations.annotations.EReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Acts simply as an intermediary to start the appropriate background service when an alarm goes off.
 * @author Eric Kok
 */
@EReceiver
public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		switch (intent.getIntExtra("service", -1)) {
		case BootReceiver.ALARM_SERVERCHECKER:
			context.startService(new Intent(context, ServerCheckerService_.class));
			break;
		case BootReceiver.ALARM_RSSCHECKER:
			context.startService(new Intent(context, RssCheckerService_.class));
			break;
		case BootReceiver.ALARM_APPUPDATES:
			context.startService(new Intent(context, AppUpdateService_.class));
			break;
		default:
			// No valid service start ID
			break;
		}
	}

}
