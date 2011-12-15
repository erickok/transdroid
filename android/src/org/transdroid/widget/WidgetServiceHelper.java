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

import java.util.List;

import org.transdroid.R;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.util.FileSizeConverter;
import org.transdroid.daemon.util.TimespanConverter;
import org.transdroid.gui.Torrents;
import org.transdroid.gui.Transdroid;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetServiceHelper {

    private static void setViewsOnWidget(Context appContext, WidgetSettings widget, RemoteViews views) {

    	AppWidgetManager manager = AppWidgetManager.getInstance(appContext);

    	// Set up a refresh intent
    	Intent intent = new Intent(appContext, WidgetService.class);//.setAction(WidgetService.INTENT_ACTION_REFRESH);
    	intent.setData(Uri.parse("widget:" + widget.getId())); // This is used to make the intent unique (see http://stackoverflow.com/questions/2844274)
    	intent.putExtra(WidgetService.INTENT_EXTRAS_WIDGET_ID, widget.getId());
    	
    	// Set up start intent (on the widget's daemon)
    	Intent start = new Intent(appContext, Torrents.class);
    	start.setData(Uri.parse("daemon:" + widget.getDaemonSettings().getIdString())); // This is used to make the intent unique (see http://stackoverflow.com/questions/2844274)
    	start.putExtra(Transdroid.INTENT_OPENDAEMON, widget.getDaemonSettings().getIdString());

		// Attach button handlers
    	views.setOnClickPendingIntent(R.id.widget_action, PendingIntent.getActivity(appContext, 0, start, 0));
    	views.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getService(appContext, widget.getId(), intent, 0));

		manager.updateAppWidget(widget.getId(), views);
    	
    }
    
    public static void showMessageTextOnWidget(Context appContext, WidgetSettings widget, CharSequence message) {
    	
    	RemoteViews views = new RemoteViews(appContext.getPackageName(), widget.getLayoutResourceId());
		WidgetServiceHelper.showMessageText(views, message);
		setViewsOnWidget(appContext, widget, views);
        
    }

    public static void showTorrentStatisticsOnWidget(Context appContext, WidgetSettings widget, List<Torrent> torrents, boolean onlyShowTransferring, int newRssCount) {
    	
		RemoteViews views = new RemoteViews(appContext.getPackageName(), widget.getLayoutResourceId());
		WidgetServiceHelper.showTorrentStatistics(appContext.getResources(), views, torrents, onlyShowTransferring, newRssCount);
		setViewsOnWidget(appContext, widget, views);
        
    }
    
    /**
     * Helper function to show a message in the widget views instead of torrent (status) data
     * @param resourceId The string resource ID with the text message
     */
    private static void showMessageText(RemoteViews views, CharSequence message) {
    	views.setTextViewText(R.id.widget_message, message);
    	views.setInt(R.id.widget_message, "setVisibility", View.VISIBLE);
    	views.setInt(R.id.widget_downloading, "setVisibility", View.GONE);
    	views.setInt(R.id.widget_eta, "setVisibility", View.GONE);
    	views.setInt(R.id.widget_other, "setVisibility", View.GONE);
    	if (views.getLayoutId() == R.layout.appwidget_small) {
        	views.setInt(R.id.widget_seeding, "setVisibility", View.GONE);
        	views.setInt(R.id.widget_progress_container, "setVisibility", View.GONE);
        	views.setProgressBar(R.id.widget_progress, 100, 0, false);
        	views.setInt(R.id.widget_rssicon, "setVisibility", View.GONE);
        	views.setInt(R.id.widget_rssnew, "setVisibility", View.GONE);
    	}
    }

    /**
     * Helper function to show an aggregate over the torrent data inside the widget views
     * @param torrent The torrents for which to show statistics
     */
    private static void showTorrentStatistics(Resources res, RemoteViews views, List<Torrent> torrents, boolean onlyShowTransferring, int newRssCount) {
    	int downloading = 0;
    	int downloadingD = 0;
    	int downloadingU = 0;
    	int eta = -1;
    	int seeding = 0;
    	int seedingU = 0;
    	float progress = 0;
    	int other = 0;
    	for (Torrent tor : torrents) {
    		if (tor.getStatusCode() == TorrentStatus.Downloading && (!onlyShowTransferring || tor.getRateDownload() > 0)) {
    			downloading++;
    			downloadingD += tor.getRateDownload();
    			downloadingU += tor.getRateUpload();
    			progress += tor.getDownloadedPercentage();
    			eta = Math.max(eta, tor.getEta());
    		} else if (tor.getStatusCode() == TorrentStatus.Seeding && (!onlyShowTransferring || tor.getRateUpload() > 0)) {
    			seeding++;
    			seedingU += tor.getRateUpload();
    		} else {
    			other++;
    		}
    	}

    	views.setInt(R.id.widget_message, "setVisibility", View.GONE);
    	views.setInt(R.id.widget_downloading, "setVisibility", View.VISIBLE);
    	views.setInt(R.id.widget_eta, "setVisibility", View.VISIBLE);
    	views.setInt(R.id.widget_other, "setVisibility", View.VISIBLE);
    	if (views.getLayoutId() == R.layout.appwidget_small) {
        	views.setInt(R.id.widget_seeding, "setVisibility", View.VISIBLE);
        	views.setInt(R.id.widget_rssicon, "setVisibility", View.VISIBLE);
        	views.setInt(R.id.widget_rssnew, "setVisibility", View.VISIBLE);
        	views.setInt(R.id.widget_progress_container, "setVisibility", View.VISIBLE);
        	views.setTextViewText(R.id.widget_downloading, 
        			downloading + " " + res.getString(R.string.widget_downloading) + (eta >= 0? " " + 
        			TimespanConverter.getTime(eta, false): (downloading == 0? "": res.getString(R.string.widget_unknowneta))));
        	views.setTextViewText(R.id.widget_eta, 
        			FileSizeConverter.getSize(downloadingD) + res.getString(R.string.widget_persecond) + "\u2193 " + 
        			FileSizeConverter.getSize(downloadingU) + res.getString(R.string.widget_persecond) + "\u2191");
        	views.setTextViewText(R.id.widget_seeding, 
        			seeding + " " + res.getString(R.string.widget_seeding) + " " + 
        			FileSizeConverter.getSize(seedingU) + res.getString(R.string.widget_persecond) + "\u2191");
        	views.setProgressBar(R.id.widget_progress, 100, (int)((progress / downloading) * 100f), false);
        	views.setTextViewText(R.id.widget_other, 
        			other + " " + res.getString(R.string.widget_other));
        	views.setTextViewText(R.id.widget_rssnew, newRssCount + " " + res.getString(R.string.widget_new));
    	} else {
        	views.setTextViewText(R.id.widget_downloading, downloading + " " + res.getString(R.string.widget_downloading) + " " + 
        			FileSizeConverter.getSize(downloadingD) + res.getString(R.string.widget_persecond) + " - " + 
        			FileSizeConverter.getSize(downloadingU) + res.getString(R.string.widget_persecond) + " " +
        			res.getString(R.string.widget_downloading_up));
        	views.setTextViewText(R.id.widget_eta, (eta == -1? res.getString(R.string.widget_unknowneta): 
        			res.getString(R.string.widget_eta) + " " + TimespanConverter.getTime(eta, true)));
        	views.setTextViewText(R.id.widget_other, seeding + " " + res.getString(R.string.widget_seeding) + " " + 
        			FileSizeConverter.getSize(seedingU) + res.getString(R.string.widget_persecond) + " - " + other + " " + 
        			res.getString(R.string.widget_other));
    	}
    }

}
