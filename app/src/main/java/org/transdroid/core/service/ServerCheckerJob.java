/*
 * Copyright 2010-2018 Eric Kok et al.
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

import android.content.Context;
import androidx.annotation.NonNull;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.app.settings.NotificationSettings_;
import org.transdroid.core.gui.log.Log_;

public class ServerCheckerJob extends Job {

	static final String TAG = "server_checker";

	private static Integer scheduledJobId;

	public static void schedule(Context context) {
		NotificationSettings notificationSettings = NotificationSettings_.getInstance_(context);
		if (notificationSettings.isEnabledForTorrents()) {
			Log_.getInstance_(context).d(TAG, "Schedule server checker job");
			NotificationChannels.ensureServerCheckerChannel(context, notificationSettings);
			scheduledJobId = new JobRequest.Builder(ServerCheckerJob.TAG)
					.setPeriodic(notificationSettings.getInvervalInMilliseconds())
					.setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
					.setUpdateCurrent(true)
					.build()
					.schedule();
		} else if (scheduledJobId != null) {
			Log_.getInstance_(context).d(TAG, "Cancel server checker job");
			JobManager.instance().cancel(scheduledJobId);
		}
	}

	@NonNull
	@Override
	protected Result onRunJob(@NonNull Params params) {
		return ServerCheckerJobRunner_.getInstance_(getContext()).run();
	}

}
