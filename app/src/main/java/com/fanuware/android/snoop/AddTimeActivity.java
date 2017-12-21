package com.fanuware.android.snoop;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.transition.TransitionInflater;
import android.util.ArrayMap;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.fanuware.android.snoop.data.WakeUpContract;
import com.fanuware.android.snoop.data.WakeUpDbHelper;
import com.fanuware.android.snoop.utils.AlarmUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


public class AddTimeActivity extends AppCompatActivity {

    private Map<Integer, ToggleButton> mDayButtons;
    private Button mSubmitTimeButton;
    private ToggleButton mAutoSnoozeButton;
    private TextView mAutoSnoozeText;
    private TimePicker mTimePicker;
    private EditText mNameEditText;
    private Integer mWakeUpId;
    private Button mPickDateButton;
    private TextView mDateTextView;
    private Calendar mTimeCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_time);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // edit alarm
        if (getIntent().hasExtra("WAKE_UP_ID")) {
            mWakeUpId = getIntent().getIntExtra("WAKE_UP_ID", 0);
            setTitle(R.string.edit_time_activity_title);

            // make curved transition
            getWindow().setSharedElementEnterTransition(TransitionInflater.from(this)
                    .inflateTransition(R.transition.tr_curve)
                    .setDuration(200));
        } else {

            // new alarm
            setTitle(R.string.add_new_time_activity_title);
        }

        // name
        mNameEditText = (EditText) findViewById(R.id.nameEditText);

        // time picker
        mTimePicker = (TimePicker) findViewById(R.id.timePicker);
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(this));

        // find all day buttons
        mDayButtons = new ArrayMap<>();
        mDayButtons.put(Calendar.SUNDAY, (ToggleButton) findViewById(R.id.sundayButton));
        mDayButtons.put(Calendar.MONDAY, (ToggleButton) findViewById(R.id.mondayButton));
        mDayButtons.put(Calendar.TUESDAY, (ToggleButton) findViewById(R.id.tuesdayButton));
        mDayButtons.put(Calendar.WEDNESDAY, (ToggleButton) findViewById(R.id.wednesdayButton));
        mDayButtons.put(Calendar.THURSDAY, (ToggleButton) findViewById(R.id.thursdayButton));
        mDayButtons.put(Calendar.FRIDAY, (ToggleButton) findViewById(R.id.fridayButton));
        mDayButtons.put(Calendar.SATURDAY, (ToggleButton) findViewById(R.id.saturdayButton));
        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            mDayButtons.get(day).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDateTextView.setVisibility(View.GONE);
                    Calendar cal = Calendar.getInstance();
                    mTimeCalendar.set(
                            cal.get(Calendar.YEAR) - 1,
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH));
                }
            });
        }

        // mode button
        mAutoSnoozeButton = (ToggleButton) findViewById(R.id.autoSnoozeButton);
        mAutoSnoozeText = (TextView) findViewById(R.id.auto_snooze_text);
        mAutoSnoozeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAutoSnoozeButton.isChecked()) {
                    mAutoSnoozeText.setText(getString(R.string.alarm_mode_auto_snooze));
                } else {
                    mAutoSnoozeText.setText(getString(R.string.alarm_mode_standard));
                }
            }
        });

        // date text view
        // note: invisible by default, unless future date retrieved (see below)
        mDateTextView = (TextView) findViewById(R.id.date_text_view);
        mDateTextView.setVisibility(View.GONE);

        // calendar
        mTimeCalendar = Calendar.getInstance();


        // get wake up id when changing a wake up
        if (mWakeUpId != null) {

            // Build appropriate uri with String row id appended
            String stringId = Integer.toString(mWakeUpId);
            Cursor cursor = getContentResolver().query(WakeUpContract.WakeUpEntry.CONTENT_URI,
                    null,
                    "_id=?",
                    new String[]{stringId},
                    null);
            cursor.moveToFirst();

            mNameEditText.setText(cursor.getString(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_NAME)));

            // retrieve hours and minutes
            String timeString = cursor.getString(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_TIME));

            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                mTimeCalendar.setTime(df.parse(timeString));
            } catch (ParseException e) {
                throw new IllegalArgumentException("Time format unknown: " + timeString);
            }

            // set hour and minutes for time picker
            mTimePicker.setHour(mTimeCalendar.get(Calendar.HOUR_OF_DAY));
            mTimePicker.setMinute(mTimeCalendar.get(Calendar.MINUTE));

            // display day buttons
            int daysMask = cursor.getInt(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_DAYS));
            for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                mDayButtons.get(day).setChecked(((WakeUpDbHelper.getDayBitMask(day) & daysMask) > 0));
            }

            // display date only if date in future
            if (daysMask <= 0
                    && mTimeCalendar.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_pattern));
                mDateTextView.setText(dateFormat.format(mTimeCalendar.getTime()));
                mDateTextView.setVisibility(View.VISIBLE);
            }

            // auto snooze mode
            int mode = cursor.getInt(cursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_MODE));
            mAutoSnoozeButton.setChecked(mode == WakeUpContract.MODE_AUTO_SNOOZE);
            mAutoSnoozeText.setText(mode == WakeUpContract.MODE_AUTO_SNOOZE
                    ? getString(R.string.alarm_mode_auto_snooze)
                    : getString(R.string.alarm_mode_standard));
        }

        // submit button
        mSubmitTimeButton = (Button) findViewById(R.id.submitTimeButton);
        mSubmitTimeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                // get all active days
                List<Integer> activeDays = new ArrayList<Integer>();
                for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                    ToggleButton button = mDayButtons.get(day);
                    if (button.isChecked()) {
                        int activeDay;
                        switch(button.getId()) {
                            case R.id.sundayButton:
                                activeDay = Calendar.SUNDAY;
                                break;
                            case R.id.mondayButton:
                                activeDay = Calendar.MONDAY;
                                break;
                            case R.id.tuesdayButton:
                                activeDay = Calendar.TUESDAY;
                                break;
                            case R.id.wednesdayButton:
                                activeDay = Calendar.WEDNESDAY;
                                break;
                            case R.id.thursdayButton:
                                activeDay = Calendar.THURSDAY;
                                break;
                            case R.id.fridayButton:
                                activeDay = Calendar.FRIDAY;
                                break;
                            case R.id.saturdayButton:
                                activeDay = Calendar.SATURDAY;
                                break;
                            default:
                                throw new IllegalArgumentException("Day not found, id: " + button.getId());
                        }
                        activeDays.add(activeDay);
                    }
                }

                // update content resolver (database)
                int dayMask = 0;
                for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                    dayMask = activeDays.contains(day) ? dayMask | (WakeUpDbHelper.getDayBitMask(day)) : dayMask;
                }

                // set calendar
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (dayMask > 0) {
                    mTimeCalendar = Calendar.getInstance();
                }
                mTimeCalendar.set(Calendar.HOUR_OF_DAY, mTimePicker.getHour());
                mTimeCalendar.set(Calendar.MINUTE, mTimePicker.getMinute());

                // write to database
                ContentValues contentValues = new ContentValues();
                contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_TIME, df.format(mTimeCalendar.getTime()));
                contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_STATE, WakeUpContract.STATE_ALARM_ON);
                contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_MODE,
                        mAutoSnoozeButton.isChecked() ? WakeUpContract.MODE_AUTO_SNOOZE : WakeUpContract.MODE_DEFAULT );
                contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_DAYS, dayMask);
                contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_NAME, mNameEditText.getText().toString());
                contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_SNOOZE_TIMESTAMP, WakeUpContract.SNOOZE_TIMESTAMP_OFF);

                if (mWakeUpId == null) {
                    getContentResolver().insert(WakeUpContract.WakeUpEntry.CONTENT_URI, contentValues);
                    Cursor cursor = getContentResolver().query(WakeUpContract.WakeUpEntry.CONTENT_URI,
                            null,
                            null,
                            null,
                            null);
                    cursor.moveToLast();
                    mWakeUpId = cursor.getInt(cursor.getColumnIndex(WakeUpContract.WakeUpEntry._ID));
                } else {
                    getContentResolver().update(WakeUpContract.WakeUpEntry.CONTENT_URI,
                            contentValues,
                            "_id=?",
                            new String[]{Integer.toString(mWakeUpId)});
                }

                // actualize next alarm
                AlarmUtils.startNextAlarm(getApplicationContext());
                AlarmUtils.toastNextAlarm(getApplicationContext(), mWakeUpId);

                // back to main activity
                finishAfterTransition();
            }
        });

        // date picker
        mPickDateButton = (Button) findViewById(R.id.pick_date);
        final DatePickerDialog datePickerDialog;
        datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                // set date
                mTimeCalendar.set(year, monthOfYear, dayOfMonth);
                SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_pattern));
                mDateTextView.setText(dateFormat.format(mTimeCalendar.getTime()));
                mDateTextView.setVisibility(View.VISIBLE);

                // reset all day buttons
                for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                    mDayButtons.get(day).setChecked(false);
                }
            }
        }, mTimeCalendar.get(Calendar.YEAR),
                mTimeCalendar.get(Calendar.MONTH),
                mTimeCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(
                mTimeCalendar.getTimeInMillis() + DateUtils.DAY_IN_MILLIS);

        // date button clicked
        mPickDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog.show();
            }
        });

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // finish without transition
            finishAfterTransition();
            onBackPressed();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {

            // finish without transition
            finishAfterTransition();
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
