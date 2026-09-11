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
package org.transdroid.background

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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.transdroid.AppContainer
import org.transdroid.MainActivity
import org.transdroid.R
import org.transdroid.appContainer
import org.transdroid.data.ServerProfile
import org.transdroid.protocol.Torrent
import org.transdroid.widget.widgetConfiguredServerIds

/**
 * Periodically checks the active server and notifies for torrents that finished since the
 * previous check. Also refreshes the home screen widget snapshot for the active server and,
 * since this is otherwise the only thing that ever runs in the background, for any other server
 * a placed widget instance is configured to show.
 */
class FinishedTorrentsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val profiles = container.profilesRepository.profiles.first()
        val activeProfile = container.activeProfile.first()

        activeProfile?.let { profile ->
            val torrents = fetchAndStore(container, profile) ?: return@let
            if (container.settingsRepository.notifyFinished.first()) {
                checkFinished(container, profile, torrents)
            }
        }

        widgetConfiguredServerIds(applicationContext)
            .filterNot { it == activeProfile?.id }
            .mapNotNull { serverId -> profiles.firstOrNull { it.id == serverId } }
            .forEach { profile -> fetchAndStore(container, profile) }

        return Result.success()
    }

    /** Fetches [profile]'s torrents and stores them for its widgets; null (and untouched) if unreachable. */
    private suspend fun fetchAndStore(container: AppContainer, profile: ServerProfile): List<Torrent>? {
        val torrents = try {
            container.adapterFor(profile).listTorrents()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        container.widgetStateRepository.update(profile.id, profile.displayName, torrents)
        return torrents
    }

    private suspend fun checkFinished(container: AppContainer, profile: ServerProfile, torrents: List<Torrent>) {
        val previouslyUnfinished = container.settingsRepository.unfinishedTorrentIds(profile.id).first()
        val newlyFinished = torrents.filter { it.isFinished && it.id in previouslyUnfinished }
        if (newlyFinished.isNotEmpty()) {
            notifyFinished(newlyFinished.map { it.name })
        }
        container.settingsRepository.setUnfinishedTorrentIds(
            profile.id,
            torrents.filterNot { it.isFinished }.map { it.id }.toSet(),
        )
    }

    // Permission is checked just below; lint cannot follow the SDK_INT-guarded early return
    @android.annotation.SuppressLint("MissingPermission", "NotificationPermission")
    private fun notifyFinished(names: List<String>) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = context.resources.getQuantityString(R.plurals.notification_finished_title, names.size, names.size)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(names.joinToString())
            .setStyle(NotificationCompat.BigTextStyle().bigText(names.joinToString("\n")))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_finished),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }

    companion object {
        private const val WORK_NAME = "finished_torrents_check"
        private const val CHANNEL_ID = "torrents_finished"
        private const val NOTIFICATION_ID = 1

        /** Idempotent; safe to call on every app start. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FinishedTorrentsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
