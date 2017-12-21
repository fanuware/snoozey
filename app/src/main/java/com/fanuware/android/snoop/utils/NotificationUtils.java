package com.fanuware.android.snoop.utils;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.fanuware.android.snoop.AlarmReceiver;
import com.fanuware.android.snoop.MainActivity;
import com.fanuware.android.snoop.R;
import com.fanuware.android.snoop.data.WakeUpContract;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class NotificationUtils {

    // notification id
    public static final int ALARM_SNOOZE_NOTIFICATION_ID = 3004;
    public static final int ALARM_CANCEL_NOTIFICATION_ID = 3204;

    /**
     * Constructs and displays a notification when an alarm is active
     *
     * @param context Context used to query our ContentProvider and use various Utility methods
     */
    public static void notifyAlarmSnooze(Context context) {

        // Get a reference to the NotificationManager
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // find all alarm snoozes
        Cursor cursor = context.getContentResolver().query(WakeUpContract.WakeUpEntry.CONTENT_URI,
                null,
                WakeUpContract.WakeUpEntry.COLUMN_SNOOZE_TIMESTAMP+"<>?",
                new String[]{WakeUpContract.SNOOZE_TIMESTAMP_OFF},
                null);

        // snooze active
        if (cursor.moveToFirst()) {

            // retrieve time
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            Calendar nextAlarm = AlarmUtils.getNextAlarm(context, AlarmUtils.findUpcomingAlarmCursor(context)); // returns null pointer (findUpcomingAlarmCursor)
            String timeString = timeFormat.format(nextAlarm.getTime());

            Resources resources = context.getResources();
            int largeArtResourceId = R.drawable.ic_alarm_default;

            Bitmap largeIcon = BitmapFactory.decodeResource(
                    resources,
                    largeArtResourceId);

            String notificationTitle = context.getResources().getQuantityString(
                    R.plurals.notification_alarm_snooze_title,
                    cursor.getCount(), cursor.getCount());

            String notificationText = context.getResources().getString(
                    R.string.notification_alarm_snooze_text, timeString);

            int smallArtResourceId = R.drawable.ic_alarm_default;

            /*
             * NotificationCompat Builder is a very convenient way to build backward-compatible
             * notifications. In order to use it, we provide a context and specify a color for the
             * notification, a couple of different icons, the title for the notification, and
             * finally the text of the notification, which in our case in a summary of today's
             * forecast.
             */
            // Use NotificationCompat.Builder to begin building the notification
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setColor(ContextCompat.getColor(context,R.color.colorPrimary))
                    .setSmallIcon(smallArtResourceId)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setAutoCancel(false);

            // content intent
            Intent mainActivityIntent = new Intent(context, MainActivity.class);
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
            taskStackBuilder.addNextIntentWithParentStack(mainActivityIntent);
            PendingIntent resultPendingIntent = taskStackBuilder
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.setContentIntent(resultPendingIntent);

            // delete intent
            Intent deleteIntent = new Intent(context, AlarmReceiver.class);
            deleteIntent.putExtra("REMOVE_ALL_SNOOZE", true);
            PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, ALARM_CANCEL_NOTIFICATION_ID,
                    deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.setDeleteIntent(deletePendingIntent);

            /*
            // delete intent
            Intent deleteIntent = new Intent(context, AlarmReceiver.class);
            deleteIntent.putExtra("REMOVE_ALL_SNOOZE", true);
            PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, ALARM_CANCEL_NOTIFICATION_ID,
                    deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder.setDeleteIntent(deletePendingIntent);

            // content intent
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String alarmModeString = sp.getString(context.getString(R.string.pref_alarm_mode_key),
                    context.getResources().getString(R.string.pref_alarm_mode_default));
            if (alarmModeString.equals(context.getString(R.string.pref_alarm_mode_value1))) {
                Intent mainActivityIntent = new Intent(context, MainActivity.class);
                TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
                taskStackBuilder.addNextIntentWithParentStack(mainActivityIntent);
                PendingIntent resultPendingIntent = taskStackBuilder
                        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                notificationBuilder.setContentIntent(resultPendingIntent);
            } else {
                notificationBuilder.setContentIntent(deletePendingIntent);
            }
            */

            // Notify the user with the ID ALARM_SNOOZE_NOTIFICATION_ID
            notificationManager.notify(ALARM_SNOOZE_NOTIFICATION_ID, notificationBuilder.build());

        } else {

            // snooze inactive
            notificationManager.cancel(ALARM_SNOOZE_NOTIFICATION_ID);
        }
    }
}
