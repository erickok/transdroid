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
 package org.transdroid.gui.util;

import org.transdroid.R;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class ErrorLogSender {

    public static final String LOG_COLLECTOR_PACKAGE_NAME = "com.xtralogic.android.logcollector";//$NON-NLS-1$
    public static final String ACTION_SEND_LOG = "com.xtralogic.logcollector.intent.action.SEND_LOG";//$NON-NLS-1$
    public static final String EXTRA_SEND_INTENT_ACTION = "com.xtralogic.logcollector.intent.extra.SEND_INTENT_ACTION";//$NON-NLS-1$
    public static final String EXTRA_DATA = "com.xtralogic.logcollector.intent.extra.DATA";//$NON-NLS-1$
    public static final String EXTRA_ADDITIONAL_INFO = "com.xtralogic.logcollector.intent.extra.ADDITIONAL_INFO";//$NON-NLS-1$
    public static final String EXTRA_SHOW_UI = "com.xtralogic.logcollector.intent.extra.SHOW_UI";//$NON-NLS-1$
    public static final String EXTRA_FILTER_SPECS = "com.xtralogic.logcollector.intent.extra.FILTER_SPECS";//$NON-NLS-1$
    public static final String EXTRA_FORMAT = "com.xtralogic.logcollector.intent.extra.FORMAT";//$NON-NLS-1$
    public static final String EXTRA_BUFFER = "com.xtralogic.logcollector.intent.extra.BUFFER";//$NON-NLS-1$

    public static void collectAndSendLog(final Context context, final IDaemonAdapter daemon, final DaemonSettings daemonSettings){
        final Intent intent = new Intent(ACTION_SEND_LOG);
        final boolean isInstalled = ActivityUtil.isIntentAvailable(context, intent);
        
        if (!isInstalled){
            new AlertDialog.Builder(context)
            .setTitle("Transdroid")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setMessage(R.string.lc_install)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int whichButton){
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:" + LOG_COLLECTOR_PACKAGE_NAME));
                    marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (ActivityUtil.isIntentAvailable(context, marketIntent)) {
                    	context.startActivity(marketIntent);
                    } else {
						Toast.makeText(context, R.string.oifm_nomarket, Toast.LENGTH_LONG).show();
                    }
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        }
        else
        {
            new AlertDialog.Builder(context)
            .setTitle("Transdroid")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setMessage(R.string.lc_run)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int whichButton){
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(EXTRA_SEND_INTENT_ACTION, Intent.ACTION_SENDTO);
                    intent.putExtra(EXTRA_DATA, Uri.parse("mailto:transdroid.org@gmail.com"));
                    intent.putExtra(EXTRA_ADDITIONAL_INFO, "My problem:\n\n\nTransdroid version " + ActivityUtil.getVersionNumber(context) + "\n" + daemon.getType().toString() + " settings: " + daemonSettings.getHumanReadableIdentifier() + "\n");
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Application failure report");
                    
                    intent.putExtra(EXTRA_FORMAT, "time");
                    
                    //The log can be filtered to contain data relevant only to your app
                    String[] filterSpecs = new String[4];
                    filterSpecs[0] = "AndroidRuntime:E";
                    filterSpecs[1] = "Transdroid:*";
                    filterSpecs[2] = "ActivityManager:*";
                    filterSpecs[3] = "*:S";
                    intent.putExtra(EXTRA_FILTER_SPECS, filterSpecs);
                    
                    context.startActivity(intent);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        }
    }

}
