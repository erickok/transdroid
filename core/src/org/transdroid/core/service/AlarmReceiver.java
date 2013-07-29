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
