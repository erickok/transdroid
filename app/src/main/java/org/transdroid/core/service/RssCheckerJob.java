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

import static java.security.AccessController.getContext;

import android.content.Context;

import androidx.annotation.NonNull;

import androidx.work.Constraints;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkManager;
import androidx.work.NetworkType;
import androidx.work.WorkerParameters;

import org.transdroid.core.app.settings.NotificationSettings;
import org.transdroid.core.app.settings.NotificationSettings_;
import org.transdroid.core.gui.log.Log_;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RssCheckerJob extends Worker {

    static final String TAG = "rss_checker";

    private static UUID scheduledJobId;

    public RssCheckerJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void schedule(Context context) {
        NotificationSettings notificationSettings = NotificationSettings_.getInstance_(context);
        if (notificationSettings.isEnabledForRss()) {
            Log_.getInstance_(context).d(TAG, "Schedule rss checker job");
            NotificationChannels.ensureRssCheckerChannel(context, notificationSettings);
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            PeriodicWorkRequest rssChecker = new PeriodicWorkRequest.Builder(RssCheckerJob.class, notificationSettings.getInvervalInMilliseconds(), TimeUnit.MILLISECONDS)
                    .addTag(RssCheckerJob.TAG)
                    .setConstraints(constraints)
                    .build();
            WorkManager.getInstance(context).cancelAllWorkByTag(RssCheckerJob.TAG);
            WorkManager.getInstance(context).enqueue(rssChecker);
            scheduledJobId = rssChecker.getId();
        } else if (scheduledJobId != null) {
            Log_.getInstance_(context).d(TAG, "Cancel rss checker job");
            WorkManager.getInstance(context).cancelWorkById(scheduledJobId);
            scheduledJobId = null;
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        return RssCheckerJobRunner_.getInstance_(getApplicationContext()).run();
    }

}
