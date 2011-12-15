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
package org.transdroid.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ifies.android.sax.Item;
import org.ifies.android.sax.RssParser;
import org.transdroid.R;
import org.transdroid.daemon.DaemonSettings;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.task.AddByFileTask;
import org.transdroid.daemon.task.AddByMagnetUrlTask;
import org.transdroid.daemon.task.AddByUrlTask;
import org.transdroid.daemon.task.DaemonTaskFailureResult;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.util.DLog;
import org.transdroid.daemon.util.Pair;
import org.transdroid.gui.Torrents;
import org.transdroid.gui.Transdroid;
import org.transdroid.gui.rss.RssFeeds;
import org.transdroid.preferences.Preferences;
import org.transdroid.rss.RssFeedSettings;
import org.transdroid.util.TLog;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.preference.PreferenceManager;

/**
 * A service that regularly checks (on alarm intents) for newly finished downloads, etc. 
 * 
 * @author erickok
 *
 */
public class AlarmService extends IntentService {

	public static final String INTENT_FORCE_CHECK = "org.transdroid.service.FORCE_CHECK";
	
	private static final String LOG_NAME = "Alarm service";
	private static final int MAX_NOTIFICATIONS = 5;
	private static final int RSS_NOTIFICATION = MAX_NOTIFICATIONS + 1; // Only use a single (overriding) notification for RSS-related messages

	private static int notificationCounter = 0;

	private static NotificationManager notificationManager;

	public AlarmService() {
		super(LOG_NAME);
		// Attach the Android TLog to the daemon logger
		DLog.setLogger(TLog.getInstance());
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {

		// Settings
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		AlarmSettings settings = Preferences.readAlarmSettings(prefs);
		List<DaemonSettings> daemonSettings = Preferences.readAllDaemonSettings(prefs);
		DaemonSettings lastUsedDaemon = Preferences.readLastUsedDaemonSettings(prefs, daemonSettings);
    	boolean onlyShowTransferring = prefs.getBoolean(Preferences.KEY_PREF_LASTSORTGTZERO, false);

		// How was the service called?
		boolean forcedStarted = intent != null && intent.getAction() != null && intent.getAction().equals(INTENT_FORCE_CHECK);
		
		// If not forcefully started, check if the user has background data disabled
		if (!forcedStarted) {
			ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			if (!conn.getBackgroundDataSetting()) {
				TLog.d(LOG_NAME, "Skip any alarm service activity, since background data is deisabled on a system-wide level");
				return;
			}
		}
		
		// Add any torrents that were queued earlier when adding them failed
		List<Pair<String, String>> queue = Preferences.readAndClearTorrentAddQueue(prefs);
		if (queue != null && queue.size() > 0) {
			String failedAgain = "";
			for (Pair<String, String> torrent : queue) {
				
				// Find the daemon where this queued torrent needs to be added to
				for (DaemonSettings daemonSetting : daemonSettings) {
					if (daemonSetting.getIdString().equals(torrent.first)) {

						DaemonTaskResult result;
						TLog.d(LOG_NAME, "Trying to add queued torrent '" + torrent.second + "' to " + daemonSetting.getHumanReadableIdentifier());
						if (Preferences.isQueuedTorrentToAddALocalFile(torrent.second)) {
							// Add this local .torrent file
							result = AddByFileTask.create(daemonSetting.getType().createAdapter(daemonSetting), torrent.second).execute();
						} else if (Preferences.isQueuedTorrentToAddAMagnetUrl(torrent.second)) {
							// Add this magnet link by URL
							result = AddByMagnetUrlTask.create(daemonSetting.getType().createAdapter(daemonSetting), torrent.second).execute();
						} else {
							// Add this web URL to a .torrent file
							result = AddByUrlTask.create(daemonSetting.getType().createAdapter(daemonSetting), torrent.second, "Queued torrent").execute();
						}
						
						// If this failed again, remember it
						// TODO: Max try, say, 5 times before finally removing it indefinitely
						if (result instanceof DaemonTaskFailureResult) {
							if (!failedAgain.equals("")) {
								failedAgain += "|";
							}
							failedAgain += torrent.first + ";" + torrent.second;
						}
						break;
					}
				}
			}
			
			// Re-add any failed torrents to the queue again (ignoring those bound to non-existing daemons)
			Preferences.addToTorrentAddQueue(prefs, failedAgain);
			
		}

		TLog.d(LOG_NAME, "Look at the servers for new torrents and started/finished downloads");
		
		// For each daemon where alarms are enabled...
		if (forcedStarted || settings.isAlarmEnabled()) {
			
			Map<String, ArrayList<Pair<String, Boolean>>> lastUpdate = Preferences.readLastAlarmStatsUpdate(prefs);
			HashMap<String, ArrayList<Pair<String, Boolean>>> thisUpdate = new HashMap<String, ArrayList<Pair<String, Boolean>>>();
			
			for (DaemonSettings daemonSetting : daemonSettings) {
				if (daemonSetting.getType() != null && (daemonSetting.shouldAlarmOnFinishedDownload() || daemonSetting.shouldAlarmOnNewTorrent())) {
					
					// Synchronously get the torrent listing
					TLog.d(LOG_NAME, daemonSetting.getHumanReadableIdentifier() + ": Retrieving torrent listing");
					ArrayList<Pair<String, Boolean>> lastStat = null;
					if (lastUpdate != null) {
						lastStat = lastUpdate.get(daemonSetting.getIdString());
					}
					IDaemonAdapter daemon = daemonSetting.getType().createAdapter(daemonSetting);
					if (daemon == null || daemon.getType() == null || daemon.getSettings() == null || daemon.getSettings().getAddress() == null) {
						// Invalid settings
						continue;
					}
					DaemonTaskResult result = RetrieveTask.create(daemon).execute();
					if (!(result instanceof RetrieveTaskSuccessResult)) {
						// If the task wasn't successful, go to the next daemon
						if (lastStat != null) {
							// Put back the old stats we knew about
							thisUpdate.put(daemonSetting.getIdString(), lastStat);
						}
						continue;
					}
					
					// With the list of torrents...
					List<Torrent> torrents = ((RetrieveTaskSuccessResult)result).getTorrents();
					TLog.d(LOG_NAME, daemonSetting.getHumanReadableIdentifier() + ": " + torrents.size() + " torrents in this update");
					if (lastUpdate == null) {
						
						TLog.d(LOG_NAME, daemonSetting.getHumanReadableIdentifier() + ": " + "No last update found");
						
					} else {
						
						// And the list of torretn in the previous server update...
						if (lastStat == null) {
							TLog.d(LOG_NAME, daemonSetting.getHumanReadableIdentifier() + ": " + "Nothing known about it in the last update");
						} else {
							TLog.d(LOG_NAME, daemonSetting.getHumanReadableIdentifier() + ": " + lastStat.size() + " torrents in last update");
							for (Torrent torrent : torrents) {
								Pair<String, Boolean> torStat = findInLastUpdate(lastStat, torrent);
								
								// Check for new torrents
								if (daemonSetting.shouldAlarmOnNewTorrent()) {
									if (torStat == null) {
										// New torrent: add to notification
										newNotification(null, getText(R.string.service_newtorrent).toString(), torrent.getName(), daemonSetting.getIdString(), Torrents.class);
									}
								}
								
								// Check if a download finished
								if (daemonSetting.shouldAlarmOnFinishedDownload()) {
									if ((torStat != null && torStat.second.equals(Boolean.FALSE) && torrent.getPartDone() == 1f) ||
											(torStat == null && torrent.getPartDone() == 1f)) {
										// Finished (and it wasn't before): add notification
										newNotification(null, getText(R.string.service_torrentfinished).toString(), torrent.getName(), daemonSetting.getIdString(), Torrents.class);
									}
								}
							
							}
						}
					}
					
					// For the last used server (by the user)...
					if (settings.showAdwNotifications() && lastUsedDaemon.getIdString().equals(daemonSetting.getIdString())) {
						
						// Count the number of (downloading) torrents
						int count = 0;
						for (Torrent torrent : torrents) {
							if ((!settings.showOnlyDownloadsInAdw() || torrent.getStatusCode() == TorrentStatus.Downloading) && 
									(!onlyShowTransferring || torrent.getRateDownload() > 0)) {
								count++;
							}
						}
						
						// Update ADW notification/counter
						TLog.d(LOG_NAME, "Update ADW counter to " + count);
						Intent iADWL = new Intent();
						iADWL.setAction("org.adw.launcher.counter.SEND");
						iADWL.putExtra("PNAME", getPackageName());
						iADWL.putExtra("COUNT", count);
						sendBroadcast(iADWL);
						
					}
					
					// Store the stats for this daemon
					ArrayList<Pair<String, Boolean>> torStats = new ArrayList<Pair<String, Boolean>>();
					for (Torrent torrent : torrents) {
						torStats.add(new Pair<String, Boolean>(torrent.getUniqueID(), torrent.getPartDone() == 1f));
					}
					thisUpdate.put(daemonSetting.getIdString(), torStats);
					
				}
			}
			
			// Store this stats update to compare to next time
			Preferences.storeLastAlarmStatsUpdate(prefs, thisUpdate);
			
			// Check for new RSS feed items
			if (settings.shouldCheckRssFeeds()) {
				List<RssFeedSettings> feeds = Preferences.readAllRssFeedSettings(prefs);
				int unread = 0;
				for (RssFeedSettings feed : feeds) {

					try {
						// Load RSS items
						RssParser parser = new RssParser(feed.getUrl());
						parser.parse();
						
						if (parser.getChannel() != null) {
							// Count items until that last known read item is found again
							// Note that the item URL is used as unique identifier
							List<Item> items = parser.getChannel().getItems();
							Collections.sort(items, Collections.reverseOrder());
							for (Item item : items) {
								if (feed.getLastNew() == null || item == null || item.getTheLink() == null || item.getTheLink().equals(feed.getLastNew())) {
									break;
								}
								unread++;
							}
						}
						
					} catch (Exception e) {
						// Ignore RSS items that could not be retrieved or parsed
					}
					
				}
				
				if (unread > 0) {
					newNotification(null, getString(R.string.service_newrssitems).toString(), getString(R.string.service_newrssitems_count, unread), null, RssFeeds.class);
				}
			}
			
		}

	}

	private void newNotification(String ticker, String title, String text, String daemonConfigID, Class<?> intentStart) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		AlarmSettings settings = Preferences.readAlarmSettings(prefs);
		
		// Set up an intent that will start Transdroid (and optionally change it to a specific daemon config)
		Intent i = new Intent(AlarmService.this, intentStart);
		if (daemonConfigID != null) {
			i.putExtra(Transdroid.INTENT_OPENDAEMON, daemonConfigID);
		}
				
		// Create a new notification
		int notifyID = notificationCounter;
		if (intentStart.equals(RssFeeds.class)) {
			notifyID = RSS_NOTIFICATION;
		}
		Notification newNotification = new Notification(R.drawable.icon_notification, ticker, System.currentTimeMillis());
		newNotification.flags = Notification.FLAG_AUTO_CANCEL;
		newNotification.setLatestEventInfo(getApplicationContext(), title, text, 
				PendingIntent.getActivity(getApplicationContext(), notifyID, i, 0));

		// Get the system notification manager, if not done so previously
		if (notificationManager == null) {
			notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}
		
		//if sound enabled add to notification
		if (settings.getAlarmPlaySound() && settings.getAlarmSoundURI() != null) {
			newNotification.sound = Uri.parse(settings.getAlarmSoundURI());
		}
		
		//if vibration enabled add to notification
		if (settings.getAlarmVibrate()) {
			newNotification.defaults = Notification.DEFAULT_VIBRATE;
		}
		// Send notification
		notificationManager.notify(notifyID, newNotification);

		// Never more than MAX_NOTIFICATIONS notifications active at one time
		notificationCounter++;
		notificationCounter = notificationCounter % MAX_NOTIFICATIONS;
		
	}

	private static Pair<String, Boolean> findInLastUpdate(ArrayList<Pair<String, Boolean>> lastStat, Torrent torrent) {
		if (lastStat != null) {
			for (Pair<String, Boolean> stat : lastStat) {
				if (stat.first.equals(torrent.getUniqueID())) {
					return stat;
				}
			}
		}
		return null;
	}

}
