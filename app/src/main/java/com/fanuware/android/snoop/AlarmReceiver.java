package com.fanuware.android.snoop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fanuware.android.snoop.utils.AlarmUtils;

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();
        if (action != null) {
            if (Intent.ACTION_INSTALL_PACKAGE.equals(action)) {
                ;//addShortcut(context);
            } else if (
                    Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
                            Intent.ACTION_TIME_CHANGED.equals(action) ||
                            Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                    ) {
                // after device boot, or time change => start next alarm

                // start alarm service
                AlarmUtils.resetAllSnoozes(context);
                AlarmUtils.startNextAlarm(context);
            }
        }

        else if (intent.hasExtra("CURRENT_ID")) {

            // launch activity when alarm triggered
            Intent alarmIntent = new Intent(context, AlarmActivity.class);
            alarmIntent.putExtras(intent.getExtras());
            context.startActivity(alarmIntent);
        } else if (intent.hasExtra("REMOVE_ALL_SNOOZE")) {

            // remove all snoozes (received when notification is canceled by user)
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String alarmModeString = sp.getString(context.getString(R.string.pref_alarm_mode_key),
                    context.getResources().getString(R.string.pref_alarm_mode_default));
            if (alarmModeString.equals(context.getString(R.string.pref_alarm_mode_value1))) {
                AlarmUtils.resetAllSnoozes(context);
                AlarmUtils.startNextAlarm(context);
            } else {
                Intent alarmIntent = new Intent(context, AlarmActivity.class);
                context.startActivity(alarmIntent);
            }
        }
    }
}