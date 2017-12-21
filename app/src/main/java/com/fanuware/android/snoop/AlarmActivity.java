package com.fanuware.android.snoop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.fanuware.android.snoop.data.WakeUpContract;
import com.fanuware.android.snoop.utils.AlarmUtils;

import java.util.List;


public class AlarmActivity extends FragmentActivity {

    // constants
    private static final String AUTO_SNOOZE_COUNT_KEY = "auto_snooze_count";

    private Vibrator mVibrator;
    private Ringtone mRingtone;
    private Integer mCurrentId = null;
    private boolean mIsAutoSnoozeRunning;
    private boolean mAutoSnooze = false;
    private Integer mSnoozeDelay;

    private Handler mAlarmDelayHandler;

    private BroadcastReceiver mPowerOffReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // Alarm snooze button
        Button alarmSnoozeButton = (Button) findViewById(R.id.alarm_snooze_button);
        alarmSnoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlAlarmSnooze();
            }
        });

        // Alarm snooze custom delay
        List<Pair<Integer, Integer>> li = new java.util.ArrayList<>();
        li.add(new Pair<Integer, Integer>(R.id.alarm_snooze_opt1, 2));
        li.add(new Pair<Integer, Integer>(R.id.alarm_snooze_opt2, 20));
        li.add(new Pair<Integer, Integer>(R.id.alarm_snooze_opt3, 30));
        for (final Pair<Integer, Integer> i : li) {
            Button currentButton = (Button) findViewById(i.first);
            currentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int snoozeDelay = i.second;
                    controlAlarmSnooze(snoozeDelay);
                }
            });
        }

        // get snooze delay from preference settings (default)
        Context context = getBaseContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String snoozeDelayString = sp.getString(context.getString(R.string.pref_snooze_delay_key),
                context.getResources().getString(R.string.pref_snooze_delay_default));
        mSnoozeDelay = Integer.valueOf(snoozeDelayString);

        // receive current alarm id
        mCurrentId = getIntent().getIntExtra("CURRENT_ID", 0);

        // retrieve cursor
        String stringId = Integer.toString(mCurrentId);
        Cursor cursor = getContentResolver().query(WakeUpContract.WakeUpEntry.CONTENT_URI,
                null,
                "_id=?",
                new String[]{stringId},
                null);
        cursor.moveToFirst();

        // Alarm title
        TextView alarmTitleText = (TextView) findViewById(R.id.alarm_title_text);
        alarmTitleText.setText(
                cursor.getString(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_NAME)));

        // check for auto snooze option
        mAutoSnooze = cursor.getInt(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_MODE))
                == WakeUpContract.MODE_AUTO_SNOOZE;

        // set fragment (depending on option selected)
        String alarmModeString = sp.getString(getString(R.string.pref_alarm_mode_key),
                getResources().getString(R.string.pref_alarm_mode_default));
        if (savedInstanceState != null) {
            ;
        } else if (alarmModeString.equals(getString(R.string.pref_alarm_mode_value1))) {
            AlarmStandardFragment alarmStandardFragment = new AlarmStandardFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.alarm_cancel_fragment, alarmStandardFragment)
                    .commit();
            alarmStandardFragment.setOnTriggerListener(new AlarmStandardFragment.OnTriggerListener() {
                @Override
                public void onTrigger() {
                    controlAlarmOff();

                }
            });
        } else {
            AlarmCalculationFragment alarmCalculationFragment = new AlarmCalculationFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.alarm_cancel_fragment, alarmCalculationFragment).commit();
            alarmCalculationFragment.setOnTriggerListener(new AlarmCalculationFragment.OnTriggerListener() {
                @Override
                public void onTrigger() {
                    controlAlarmOff();
                }
            });
        }

        // screen off event (triggered when power button pressed)
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);

        mPowerOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    controlAlarmSnooze();
                }
            }
        };
        registerReceiver(mPowerOffReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(mPowerOffReceiver);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // turn screen on if sleep
        Window win = getWindow();
        win.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                controlAlarmSnooze();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {

            // delay to start alarm
            if (mAlarmDelayHandler == null) {
                mAlarmDelayHandler = new Handler();
            } else {
                mAlarmDelayHandler.removeCallbacksAndMessages(null);
            }
            mAlarmDelayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    turnAlarmOn();
                }
            }, 1000);
        }
        else {
            if (mAlarmDelayHandler != null)
                mAlarmDelayHandler.removeCallbacksAndMessages(null);

            // turn alarm off
            if (mVibrator != null) {
                mVibrator.cancel();
            }
            if (mRingtone != null) {
                mRingtone.stop();
            }

            // set snooze
            if (mSnoozeDelay != null)
                AlarmUtils.setSnooze(this, mCurrentId, mSnoozeDelay);
            else
                AlarmUtils.resetSnooze(this, mCurrentId);

            // start next upcoming alarm
            AlarmUtils.startNextAlarm(getApplicationContext());

            // remove completely, no background tab
            finishAndRemoveTask();
        }
    }

    private void controlAlarmOff() {
        AlarmUtils.resetAlarm(getBaseContext(), mCurrentId);
        mSnoozeDelay = null;
        finishAndRemoveTask();
    }

    private void controlAlarmSnooze() {
        finishAndRemoveTask();
    }

    private void controlAlarmSnooze(int snoozeDelay) {
        mSnoozeDelay = snoozeDelay;
        finishAndRemoveTask();
    }

    // turn alarm on
    private void turnAlarmOn() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        // vibrate device (wait, vibrate, sleep)
        if (sp.getBoolean(getString(R.string.pref_vibration_key), true)) {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0, 100, 1000};
            mVibrator.vibrate(pattern, 0);
        }

        // consider volume preference
        String volumeString = sp.getString(
                getString(R.string.pref_volume_key),
                getString(R.string.pref_volume_default));
        if(!volumeString.equals(getString(R.string.pref_volume_default))) {
            int volume = Integer.parseInt(volumeString);
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume (
                    AudioManager.STREAM_ALARM,
                    (audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) - 1)
                            * volume / 100 + 1,
                    0);
        } // else, default device volume

        // play sound
        SharedPreferences sharedPref = getSharedPreferences("snap", MODE_PRIVATE);
        String ringtoneUriString = sharedPref.getString("ringtone",
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
        Uri ringtoneUri = Uri.parse(ringtoneUriString);
        mRingtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
        mRingtone.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).build());
        mRingtone.play();

        // set max alarm time
        int alarmDurationMillis;
        try {

            // maximum alarm duration
            String snoozeRepetitionString = sp.getString(getString(R.string.pref_max_alarm_duration_key),
                    getResources().getString(R.string.pref_max_alarm_duration_default));
            alarmDurationMillis = Integer.valueOf(snoozeRepetitionString) * 1000;
        } catch (NumberFormatException e) {

            // indicate unlimited duration
            alarmDurationMillis = 86400000;
        };

        // any auto snooze running
        Cursor cursor = getContentResolver().query(
                WakeUpContract.WakeUpEntry.CONTENT_URI,
                null,
                WakeUpContract.WakeUpEntry.COLUMN_SNOOZE_TIMESTAMP+"<>?",
                new String[]{WakeUpContract.SNOOZE_TIMESTAMP_OFF},
                null);
        boolean anySnoozeAlreadyRunning = cursor != null && cursor.getCount() >= 1;

        // retrieve counter
        int autoSnoozeCounter;
        if (!anySnoozeAlreadyRunning) {
            autoSnoozeCounter = 0;
        } else {
            autoSnoozeCounter = sharedPref.getInt(AUTO_SNOOZE_COUNT_KEY, 0);
        }

        int autoSnoozeRepetition = Integer.parseInt(
                sp.getString(
                        getString(R.string.pref_auto_snooze_repetition_key),
                        getString(R.string.pref_auto_snooze_repetition_default)
                ));

        // continue auto snooze
        mIsAutoSnoozeRunning = (mAutoSnooze && autoSnoozeCounter < autoSnoozeRepetition);
        if (mIsAutoSnoozeRunning) {
            int autoSnoozeDelay = Integer.parseInt(
                    sp.getString(
                            getString(R.string.pref_auto_snooze_duration_key),
                            getString(R.string.pref_auto_snooze_duration_default)
                    ));
            alarmDurationMillis = autoSnoozeDelay * 1000;
            autoSnoozeCounter++;
        }

        // update counter
        SharedPreferences.Editor editPref = sharedPref.edit();
        editPref.putInt(AUTO_SNOOZE_COUNT_KEY, autoSnoozeCounter);
        editPref.commit();

        // start maximum alarm timer
        new CountDownTimer(alarmDurationMillis, 100) {

            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                if (mIsAutoSnoozeRunning)
                    controlAlarmSnooze();
                else
                    controlAlarmOff();
            }
        }.start();
    }
}
