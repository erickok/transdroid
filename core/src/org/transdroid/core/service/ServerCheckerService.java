package org.transdroid.core.service;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.transdroid.core.R;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.gui.TorrentsActivity_;
import org.transdroid.core.gui.log.Log;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.RetrieveTask;
import org.transdroid.daemon.task.RetrieveTaskSuccessResult;
import org.transdroid.daemon.util.Collections2;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.InboxStyle;

/**
 * A background service that checks all user-configured servers (if so desired) for new and finished torrents.
 * @author Eric Kok
 */
@EService
public class ServerCheckerService extends IntentService {

	@Bean
	protected ConnectivityHelper connectivityHelper;
	@Bean
	protected NotificationSettings notificationSettings;
	@Bean
	protected ApplicationSettings applicationSettings;
	@SystemService
	protected NotificationManager notificationManager;

	public ServerCheckerService() {
		super("ServerCheckerService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		if (!connectivityHelper.shouldPerformActions() || !notificationSettings.isEnabled()) {
			Log.d(this,
					"Skip the server checker service, as background data is disabled, the service is disabled or we are not connected.");
			return;
		}

		int notifyBase = 10000;
		for (ServerSetting server : applicationSettings.getServerSettings()) {

			// No need to check if the server is not properly configured or none of the two types of notifications are
			// enabled by the user for this specific server
			if (server.getType() == null || server.getAddress() == null || server.getAddress().equals("")
					|| !(server.shouldAlarmOnFinishedDownload() || server.shouldAlarmOnNewTorrent()))
				return;

			// Get the statistics for the last time we checked this server
			JSONArray lastStats = applicationSettings.getServerLastStats(server);

			// Synchronously retrieve torrents listing
			IDaemonAdapter adapter = server.createServerAdapter();
			DaemonTaskResult result = RetrieveTask.create(adapter).execute();
			if (!(result instanceof RetrieveTaskSuccessResult)) {
				// Cannot retrieve torrents at this time
				return;
			}
			List<Torrent> retrieved = ((RetrieveTaskSuccessResult) result).getTorrents();
			Log.d(this, server.getName() + ": Retrieved torrent listing");

			// Check for differences between the last and the current stats
			JSONArray currentStats = new JSONArray();
			List<Torrent> newTorrents = new ArrayList<Torrent>();
			List<Torrent> doneTorrents = new ArrayList<Torrent>();
			for (Torrent torrent : retrieved) {

				// Remember this torrent for the next time
				try {
					currentStats.put(new JSONObject().put("id", torrent.getUniqueID()).put("done",
							torrent.getPartDone() == 1F));
				} catch (JSONException e) {
					// Can't build the JSON object; this should not happen and we can safely ignore it
				}
				
				// See if this torrent was done the last time we checked
				if (lastStats != null) {
					Boolean wasDone = findLastDoneStat(lastStats, torrent);
					if (server.shouldAlarmOnNewTorrent() && wasDone == null) {
						// This torrent wasn't present earlier
						newTorrents.add(torrent);
						continue;
					}
					if (server.shouldAlarmOnFinishedDownload() && torrent.getPartDone() == 1F && wasDone != null && !wasDone)
						// This torrent is now done, but wasn't before
						doneTorrents.add(torrent);
				}
				
			}

			// Store the now-current statistics on torrents for the next time we check this server
			applicationSettings.setServerLastStats(server, currentStats);

			// Notify on new and now-done torrents for this server
			Log.d(this, server.getName() + ": " + newTorrents.size() + " new torrents, " + doneTorrents.size()
					+ " newly finished torrents.");
			Intent i = new Intent(this, TorrentsActivity_.class);
			i.putExtra("org.transdroid.START_SERVER", server.getOrder());
			// Should start the main activity directly into this server
			PendingIntent pi = PendingIntent.getActivity(this, notifyBase + server.getOrder(), i,
					Intent.FLAG_ACTIVITY_NEW_TASK);
			ArrayList<Torrent> affectedTorrents = new ArrayList<Torrent>(newTorrents.size() + doneTorrents.size());
			affectedTorrents.addAll(newTorrents);
			affectedTorrents.addAll(doneTorrents);
			String title, forString = Collections2.joinString(affectedTorrents, ", ");
			if (newTorrents.size() > 0 && doneTorrents.size() > 0) {
				// Note: use the 'one' plural iif 1 new torrent was added and 1 was newly finished
				title = getResources().getQuantityString(R.plurals.status_service_finished,
						newTorrents.size() + doneTorrents.size() == 2 ? 1 : 2, Integer.toString(newTorrents.size()),
						Integer.toString(doneTorrents.size()));
			} else if (newTorrents.size() > 0) {
				title = getResources().getQuantityString(R.plurals.status_service_added, newTorrents.size(),
						Integer.toString(newTorrents.size()));
			} else if (doneTorrents.size() > 0) {
				title = getResources().getQuantityString(R.plurals.status_service_finished, doneTorrents.size(),
						Integer.toString(doneTorrents.size()));
			} else {
				// No notification to show
				continue;
			}

			// Build the basic notification
			Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_stat_notification)
					.setTicker(title).setContentTitle(title).setContentText(forString)
					.setNumber(affectedTorrents.size())
					.setLights(notificationSettings.getDesiredLedColour(), 600, 1000)
					.setSound(notificationSettings.getSound()).setAutoCancel(true).setContentIntent(pi);
			if (notificationSettings.shouldVibrate())
				builder.setVibrate(notificationSettings.getDefaultVibratePattern());

			// Add at most 5 lines with the affected torrents
			InboxStyle inbox = new NotificationCompat.InboxStyle(builder);
			if (affectedTorrents.size() < 6) {
				for (Torrent affectedTorrent : affectedTorrents) {
					inbox.addLine(affectedTorrent.getName());
				}
			} else {
				for (int j = 0; j < 4; j++) {
					inbox.addLine(affectedTorrents.get(j).getName());
				}
				inbox.addLine(getString(R.string.status_service_andothers, affectedTorrents.get(5).getName()));
			}
			notificationManager.notify(notifyBase + server.getOrder(), inbox.build());

		}

	}

	private Boolean findLastDoneStat(JSONArray lastStats, Torrent torrent) {
		for (int i = 0; i < lastStats.length(); i++) {
			try {
				if (lastStats.getJSONObject(i).getString("id").equals(torrent.getUniqueID()))
					return lastStats.getJSONObject(i).getBoolean("done");
			} catch (JSONException e) {
				return null;
			}
		}
		return null;
	}

}
