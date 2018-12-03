package org.transdroid.core.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import org.transdroid.R;

class NotificationChannels {

    public static final String CHANNEL_APP_UPDATE = "channel_app_update";
    public static final String CHANNEL_RSS_CHECKER = "channel_rss_checker";
    public static final String CHANNEL_SERVER_CHECKER = "channel_server_checker";

    static void ensureAppUpdateChannel(final Context context) {
        createChannel(context, CHANNEL_APP_UPDATE, R.string.pref_checkupdates, R.string.pref_checkupdates_info);
    }

    static void ensureRssCheckerChannel(final Context context) {
        createChannel(context, CHANNEL_RSS_CHECKER, R.string.pref_notifications_rss, null);
    }

    static void ensureServerCheckerChannel(final Context context) {
        createChannel(context, CHANNEL_SERVER_CHECKER, R.string.pref_notifications_torrent, null);
    }

    private static void createChannel(
            final Context context,
            final String channelId,
            final int name,
            final Integer description) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            final NotificationChannel channel = new NotificationChannel(channelId, context.getString(name), importance);
            if (description != null) {
                channel.setDescription(context.getString(description));
            }
            final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
