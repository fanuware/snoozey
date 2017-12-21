package com.fanuware.android.snoop.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.fanuware.android.snoop.AlarmReceiver;
import com.fanuware.android.snoop.R;
import com.fanuware.android.snoop.data.WakeUpContract;
import com.fanuware.android.snoop.data.WakeUpDbHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by fabian nussbaumer on 23.12.2017.
 */

public class AlarmUtils {

    // constants
    private static final int DAYS = 7;

    // alarm manager
    public static final int ALARM_REQUEST_ID = 1204;

    /**
     * toast next recent alarm
     *
     * @param context
     * @param currentId database entry
     */
    public static void toastNextAlarm(Context context, int currentId) {
        String message;
        Resources res = context.getResources();

        // next recent alarm
        Cursor cursor = context.getContentResolver().query(WakeUpContract.WakeUpEntry.CONTENT_URI,
                null,
                "_id=?",
                new String[]{Integer.toString(currentId)},
                null);
        cursor.moveToFirst();
        Calendar clockCalendar = getNextAlarm(context, cursor);
        clockCalendar.set(Calendar.SECOND, 1);

        // current date and time
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(Calendar.SECOND, 0);

        // get time durations
        long timeDuration = clockCalendar.getTimeInMillis() - currentCalendar.getTimeInMillis();
        int daysDuration = (int) (timeDuration / DateUtils.DAY_IN_MILLIS);
        String daysDurationString = res.getQuantityString(R.plurals.numberOfDays, daysDuration, daysDuration);
        int hoursDuration = (int) (timeDuration / DateUtils.HOUR_IN_MILLIS) % 24;
        String hoursDurationString = res.getQuantityString(R.plurals.numberOfHours, hoursDuration, hoursDuration);
        int minutesDuration = (int) (timeDuration / DateUtils.MINUTE_IN_MILLIS) % 60;
        String minutesDurationString = res.getQuantityString(R.plurals.numberOfMinutes, minutesDuration, minutesDuration);

        // toast message
        if (daysDuration > 0) {
            if (hoursDuration > 0) {
                message = res.getString(R.string.alarm_message_double,
                        daysDurationString,
                        hoursDurationString);
            } else {
                message = res.getString(R.string.alarm_message_single,
                        daysDurationString);
            }
        } else {
            if (hoursDuration > 0) {
                message = res.getString(R.string.alarm_message_double,
                        hoursDurationString,
                        minutesDurationString);
            } else {
                message = res.getString(R.string.alarm_message_single,
                        minutesDurationString);
            }
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * find next upcoming and most recent alarm
     * searches through all alarm entries
     *
     * @param context
     * @return alarm cursor
     */
    public static Cursor findUpcomingAlarmCursor(Context context) {
        Cursor cursor = context.getContentResolver().query(WakeUpContract.WakeUpEntry.CONTENT_URI,
                null,
                WakeUpContract.WakeUpEntry.COLUMN_STATE+"=?",
                new String[]{Integer.toString(WakeUpContract.STATE_ALARM_ON)},
                null);

        // retrieve next upcoming alarm
        Calendar nextAlarm = null;
        int cursorPosition = 0;
        for (boolean valid = cursor.moveToFirst(); valid; valid = cursor.moveToNext()) {
            Calendar tempAlarm = getNextAlarm(context, cursor);

            // continue if no alarm found (invalid)
            if (tempAlarm == null) continue;

            // compare calendar and choose most recent one
            if (nextAlarm == null ||
                    (tempAlarm.getTimeInMillis() < nextAlarm.getTimeInMillis())) {
                nextAlarm = tempAlarm;
                cursorPosition = cursor.getPosition();
            }
        }
        if (nextAlarm == null) return null;
        cursor.moveToPosition(cursorPosition);
        return cursor;
    }

    /**
     * find next and most recent alarm
     *
     * @param context
     * @param cursor current alarm entry from database
     * @return
     */
    public static Calendar getNextAlarm(Context context, Cursor cursor) {

        // only continue when alarm is activated
        if (cursor.getInt(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_STATE))
            == WakeUpContract.STATE_ALARM_OFF) {
            return null;
        }

        // retrieve all active days
        List<Integer> activeDays = new ArrayList<>();
        int daysMask = cursor.getInt(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_DAYS));
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            if ((WakeUpDbHelper.getDayBitMask(day) & daysMask) > 0) {
                activeDays.add(day);
            }
        }

        // retrieve hours and minutes
        Calendar calendar = Calendar.getInstance();
        String timeString = cursor.getString(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_TIME));

        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            calendar.setTime(df.parse(timeString));
        } catch (ParseException e) {
            throw new IllegalArgumentException("Time format unknown: " + timeString);
        }

        // get current id
        int currentId = cursor.getInt(cursor.getColumnIndex(WakeUpContract.WakeUpEntry._ID));

        // setup date for most recent clock alarm
        return getNextAlarmHelper(context, activeDays, calendar, currentId);
    }

    /**
     * find next recent day
     * helper method with all specified parameters
     *
     * @param context
     * @param activeDays
     * @param calendar
     * @param currentId
     * @return
     */
    private static Calendar getNextAlarmHelper(Context context,
                                        List<Integer> activeDays,
                                        Calendar calendar,
                                        int currentId) {

        // second always zero
        calendar.set(Calendar.SECOND, 0);

        // current date and time
        Calendar currentCalendar = Calendar.getInstance();
        Calendar clockCalendar;

        // snooze, consider last timestamp
        Calendar lastSnooze = getLastSnooze(context, currentId);
        if (lastSnooze != null) {
            clockCalendar = lastSnooze;
        }

        // regular alarm schedule
        else {
            // setup most recent clock

            // add one day in case current time has already passed
            if (currentCalendar.getTimeInMillis() > calendar.getTimeInMillis()) {
                clockCalendar = Calendar.getInstance();
                clockCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
                clockCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
                clockCalendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND));
                while (currentCalendar.getTimeInMillis() > clockCalendar.getTimeInMillis()) {
                    clockCalendar.add(Calendar.HOUR, 24);
                }
            } else {
                clockCalendar = calendar;
            }

            // find clock for selected day
            if (!activeDays.isEmpty()) {
                for (int i = 0; i < DAYS; i++) {
                    if (activeDays.contains(clockCalendar.get(Calendar.DAY_OF_WEEK))) {
                        break;
                    }
                    clockCalendar.add(Calendar.HOUR, 24);
                }
            }
        }
        return clockCalendar;
    }

    /**
     * start next recent alarm
     *
     * @param context
     * @return next alarm cursor
     */
    public static Integer startNextAlarm(Context context) {

        // show notification
        NotificationUtils.notifyAlarmSnooze(context);

        // prepare alarm intent
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);

        // find upcoming alarm
        Cursor upcomingAlarmCursor = findUpcomingAlarmCursor(context);
        if(upcomingAlarmCursor == null) {

            // cancel current alarm
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_ID,
                    intent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d("fanu_noAlarm", "-");//nu
            return null;
        }

        // schedule alarm, only one at a time
        intent.putExtra("CURRENT_ID",
                upcomingAlarmCursor.getInt(
                        upcomingAlarmCursor.getColumnIndex(WakeUpContract.WakeUpEntry._ID)));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_ID,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Calendar upcomingAlarmCalendar = getNextAlarm(context, upcomingAlarmCursor);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, upcomingAlarmCalendar.getTimeInMillis(), pendingIntent);
        Log.d("fanu_nextAlarm", upcomingAlarmCalendar.toString());//nu
        return upcomingAlarmCursor.getInt(upcomingAlarmCursor.getColumnIndex(WakeUpContract.WakeUpEntry._ID));
    }

    /**
     * set snooze
     *
     * @param context
     * @param currentId current alarm entry
     */
    public static void setSnooze(Context context, int currentId, Integer delayMinute) {

        // update current date
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ContentValues contentValues = new ContentValues();

        // calculate snooze timestamp: time + snooze delay
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, delayMinute);
        Date delayedAlarm = calendar.getTime();
        contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_SNOOZE_TIMESTAMP,
                df.format(delayedAlarm));

        String stringId = Integer.toString(currentId);
        Uri currentUri = WakeUpContract.WakeUpEntry.CONTENT_URI;
        currentUri = currentUri.buildUpon().appendPath(stringId).build();
        context.getContentResolver().update(currentUri,
                contentValues,
                null,
                null);
    }

    // reset snooze
    public static void resetSnooze(Context context, int currentId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_SNOOZE_TIMESTAMP,
                WakeUpContract.SNOOZE_TIMESTAMP_OFF);

        String stringId = Integer.toString(currentId);
        Uri currentUri = WakeUpContract.WakeUpEntry.CONTENT_URI;
        currentUri = currentUri.buildUpon().appendPath(stringId).build();
        context.getContentResolver().update(currentUri,
                contentValues,
                null,
                null);
    }

    /**
     * reset snooze
     *
     * @param context
     */
    public static void resetAllSnoozes(Context context) {

        // find all alarm snoozes
        Cursor cursor = context.getContentResolver().query(WakeUpContract.WakeUpEntry.CONTENT_URI,
                null,
                WakeUpContract.WakeUpEntry.COLUMN_SNOOZE_TIMESTAMP+"<>?",
                new String[]{WakeUpContract.SNOOZE_TIMESTAMP_OFF},
                null);
        for (boolean valid = cursor.moveToFirst(); valid; valid = cursor.moveToNext()) {
            resetSnooze(context, cursor.getInt(cursor.getColumnIndex(WakeUpContract.WakeUpEntry._ID)));
        }
    }

    /**
     * get snooze
     *
     * @param context
     * @param currentId current alarm entry
     * @return
     */
    public static Calendar getLastSnooze(Context context, int currentId) {

        // query snooze from db
        String stringId = Integer.toString(currentId);
        Cursor cursor = context.getContentResolver().query(WakeUpContract.WakeUpEntry.CONTENT_URI,
                null,
                "_id=?",
                new String[]{stringId},
                null);

        // continue only when entry available
        if (cursor == null) return null;
        cursor.moveToFirst();

        // retrieve calendar
        Calendar calendar = Calendar.getInstance();
        String timeString = cursor.getString(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_SNOOZE_TIMESTAMP));
        if(!timeString.equals(WakeUpContract.SNOOZE_TIMESTAMP_OFF)) {

            // snooze active
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                calendar.setTime(df.parse(timeString));
                return calendar;
            } catch (ParseException e) {
                throw new IllegalArgumentException("Time format unknown: " + timeString);
            }
        } else {

            // snooze inactive
            return null;
        }
    }

    /**
     * reset alarm, turn off
     *
     * @param context
     * @param currentId current alarm cursor
     */
    public static void resetAlarm(Context context, Integer currentId) {
        if (currentId == null) return;

        // reset snooze
        resetSnooze(context, currentId);

        // if no day selected, turn alarm off (it fires only once)
        String stringId = Integer.toString(currentId);
        Cursor cursor = context.getContentResolver().query(WakeUpContract.WakeUpEntry.CONTENT_URI,
                null,
                "_id=?",
                new String[]{stringId},
                null);
        if (cursor.moveToFirst() &&
                (cursor.getInt(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_DAYS)) == 0)) {

            // state alarm off
            ContentValues contentValues = new ContentValues();
            contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_STATE,
                    WakeUpContract.STATE_ALARM_OFF);
            Uri uri = WakeUpContract.WakeUpEntry.CONTENT_URI;
            uri = uri.buildUpon().appendPath(stringId).build();
            context.getContentResolver().update(uri,
                    contentValues,
                    null,
                    null );
        }
    }
}