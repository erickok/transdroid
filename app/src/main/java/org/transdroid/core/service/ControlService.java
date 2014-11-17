package org.transdroid.core.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.transdroid.core.app.settings.ApplicationSettings;
import org.transdroid.core.app.settings.ServerSetting;
import org.transdroid.core.gui.log.Log;
import org.transdroid.core.widget.ListWidgetConfig;
import org.transdroid.core.widget.*;
import org.transdroid.daemon.IDaemonAdapter;
import org.transdroid.daemon.task.DaemonTask;
import org.transdroid.daemon.task.DaemonTaskResult;
import org.transdroid.daemon.task.DaemonTaskSuccessResult;
import org.transdroid.daemon.task.PauseAllTask;
import org.transdroid.daemon.task.ResumeAllTask;
import org.transdroid.daemon.task.SetTransferRatesTask;
import org.transdroid.daemon.task.StartAllTask;
import org.transdroid.daemon.task.StopAllTask;

@EService
public class ControlService extends IntentService {

	// NOTE: These are the same strings as Transdroid 1, for backwards compatibility
	public static final String INTENT_SETTRANSFERRATES = "org.transdroid.control.SET_TRANSFER_RATES";
	public static final String INTENT_PAUSEALL = "org.transdroid.control.PAUSE_ALL";
	public static final String INTENT_RESUMEALL = "org.transdroid.control.RESUME_ALL";
	public static final String INTENT_STARTALL = "org.transdroid.control.START_ALL";
	public static final String INTENT_STOPALL = "org.transdroid.control.STOP_ALL";
	public static final String EXTRA_DAEMON = "DAEMON";
	public static final String EXTRA_UPLOAD_RATE = "UPLOAD_RATE";
	public static final String EXTRA_DOWNLOAD_RATE = "DOWNLOAD_RATE";

	@Bean
	protected Log log;
	@Bean
	protected ConnectivityHelper connectivityHelper;
	@Bean
	protected ApplicationSettings applicationSettings;

	public ControlService() {
		super("ControlService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		if (intent == null) {
			return;
		}

		// We should have been supplied either am EXTRA_DAEMON or an AppWidgetManager.EXTRA_APPWIDGET_ID
		ServerSetting server;
		int appWidgetId = -1;
		if (intent.hasExtra(EXTRA_DAEMON)) {

			// See if the supplied server id is pointing to a valid server
			int serverId = intent.getIntExtra(EXTRA_DAEMON, -1);
			if (serverId < 0 || serverId > applicationSettings.getMaxOfAllServers()) {
				// This server does not exist (any more) or no valid EXTRA_DAEMON value was supplied
				log.e(this, "The control service can be started with a DAEMON extra zero-based server id, but the" +
						"supplied id was invalid or no longer points to an existing server.");
				return;
			}
			server = applicationSettings.getServerSetting(serverId);

		} else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {

			// This was called directly form a home screen widget
			appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			ListWidgetConfig config = applicationSettings.getWidgetConfig(appWidgetId);
			if (config == null) {
				log.e(this,
						"The control service can be started by a widget using the AppWidgetManager.EXTRA_APPWIDGET_ID, " +
								"but the id that was supplied does not point to an existing home screen widget.");
				return;
			}
			int serverId = config.getServerId();
			if (serverId < 0 || serverId > applicationSettings.getMaxOfAllServers()) {
				log.e(this, "The home screen widget points to a server that no longer exists.");
				return;
			}
			server = applicationSettings.getServerSetting(serverId);

		} else {

			// Simply use the last-used server
			server = applicationSettings.getLastUsedServer();

		}

		// Still no server? Then we don't have one specified yet
		if (server == null) {
			log.e(this, "The control service was called, but there are nog servers configured at all.");
			return;
		}

		// See which action should be performed on the server
		IDaemonAdapter adapter = server.createServerAdapter(connectivityHelper.getConnectedNetworkName(), this);
		DaemonTask task = null;
		if (intent.getAction().equals(INTENT_RESUMEALL)) {
			task = ResumeAllTask.create(adapter);
		} else if (intent.getAction().equals(INTENT_PAUSEALL)) {
			task = PauseAllTask.create(adapter);
		} else if (intent.getAction().equals(INTENT_STARTALL)) {
			task = StartAllTask.create(adapter, false);
		} else if (intent.getAction().equals(INTENT_STOPALL)) {
			task = StopAllTask.create(adapter);
		} else if (intent.getAction().equals(INTENT_SETTRANSFERRATES)) {
			// NOTE: If the upload or download rate was not specified, it will be reset on the server instead
			int uploadRate = intent.getIntExtra(EXTRA_UPLOAD_RATE, -1);
			int downloadRate = intent.getIntExtra(EXTRA_DOWNLOAD_RATE, -1);
			task = SetTransferRatesTask
					.create(adapter, uploadRate == -1 ? null : uploadRate, downloadRate == -1 ? null : downloadRate);
		}

		// Execute the task, if we have one now
		if (task == null) {
			log.e(this, "The control service was started, but no (valid) action was specified, such as " +
					"org.transdroid.control.START_ALL or org.transdroid.control.SET_TRANSFER_RATES");
			return;
		}
		DaemonTaskResult result = task.execute(log);
		if (result instanceof DaemonTaskSuccessResult) {
			log.i(this,
					task.getMethod().name() + " was successfully executed on " + server.getHumanReadableIdentifier());
		} else {
			log.i(this, task.getMethod().name() + " was NOT succcessfully executed on " +
							server.getHumanReadableIdentifier() + " (and we are NOT trying again)");
			// No need to continue now
			return;
		}

		// The task was successful, so maybe we need to update the original calling widget now too
		if (appWidgetId >= 0) {

			// Just wait for (max) two seconds, to give the server time to finish its last action
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				// Sleep
			}

			// Ask the app widget provider to update this specific widget
			Intent update = new Intent(this, ListWidgetProvider_.class);
			update.setAction("android.appwidget.action.APPWIDGET_UPDATE");
			update.putExtra(ListWidgetProvider.EXTRA_REFRESH, appWidgetId);
			update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			sendBroadcast(update);

		}

	}
}
