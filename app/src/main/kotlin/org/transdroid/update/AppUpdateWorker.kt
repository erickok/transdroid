/*
 * Copyright 2010-2026 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid. If not, see <https://www.gnu.org/licenses/>.
 */
package org.transdroid.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import org.transdroid.BuildConfig
import org.transdroid.R
import org.transdroid.appContainer

/**
 * Checks once a day whether a newer Transdroid is out, and notifies once per new version.
 * Only scheduled in builds that may update themselves (the `updatecheck_available` bool, off
 * for app store builds) while the user hasn't turned it off in Settings.
 */
class AppUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        if (!isAvailable(applicationContext) || !container.settingsRepository.checkUpdates.first()) {
            return Result.success()
        }
        val release = try {
            AppUpdates.fetch(container.httpClient)
        } catch (e: IOException) {
            return Result.retry()
        }
        if (container.settingsRepository.recordLatestRelease(release, BuildConfig.VERSION_CODE)) {
            notifyUpdate(release)
        }
        return Result.success()
    }

    // Permission is checked just below; lint cannot follow the SDK_INT-guarded early return
    @android.annotation.SuppressLint("MissingPermission", "NotificationPermission")
    private fun notifyUpdate(release: LatestRelease) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // The About screen still shows the new version
            return
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_updates),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        val download = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_VIEW, AppUpdates.DOWNLOAD_URL.toUri()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_update_title, release.versionName))
            .setContentText(context.getString(R.string.notification_update_text))
            .setContentIntent(download)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val WORK_NAME = "app_update_check"
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 2

        fun isAvailable(context: Context): Boolean = context.resources.getBoolean(R.bool.updatecheck_available)

        /** Starts or stops the daily check to match the build and the user's setting. */
        fun schedule(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled || !isAvailable(context)) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
