/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fanuware.android.snoop;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.fanuware.android.snoop.data.WakeUpContract;
import com.fanuware.android.snoop.data.WakeUpDbHelper;
import com.fanuware.android.snoop.utils.AlarmUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

class TimesAdapter extends RecyclerView.Adapter<TimesAdapter.TimesAdapterViewHolder> {

    private Context mContext;
    private Cursor mCursor;

    final private TimesAdapterOnClickHandler mClickHandler;

    public interface TimesAdapterOnClickHandler {

        void onClick(View view, int wakeUpId);
    }

    // constructor
    public TimesAdapter(@NonNull Context context, TimesAdapterOnClickHandler clickHandler) {
        mContext = context;
        mClickHandler = clickHandler;
    }

    @Override
    public TimesAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_times_list, viewGroup, false);
        view.setFocusable(true);
        return new TimesAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final TimesAdapterViewHolder timesAdapterViewHolder, final int position) {
        mCursor.moveToPosition(position);
        final int currentId = mCursor.getInt(mCursor.getColumnIndex(WakeUpContract.WakeUpEntry._ID));

        // retrieve alarm calendar
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            calendar.setTime(
                    df.parse(
                            mCursor.getString(mCursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_TIME))
                    ));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date date = calendar.getTime();

        // set mode image
        if (mCursor.getInt(mCursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_MODE))
                == WakeUpContract.MODE_DEFAULT ) {
            timesAdapterViewHolder.mModeImageView.setImageResource(R.drawable.ic_alarm_default);
        } else {
            timesAdapterViewHolder.mModeImageView.setImageResource(R.drawable.ic_alarm_auto_snooze);
        }

        // display time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        timesAdapterViewHolder.mTimeTextView.setText(timeFormat.format(date));

        // display title
        String name = mCursor.getString(mCursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_NAME));
        if (name.isEmpty()) {
            timesAdapterViewHolder.mNameTextView.setVisibility(View.GONE);
        } else {
            timesAdapterViewHolder.mNameTextView.setVisibility(View.VISIBLE);
            timesAdapterViewHolder.mNameTextView.setPaintFlags(
                    timesAdapterViewHolder.mNameTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            timesAdapterViewHolder.mNameTextView.setText(name);
        }

        // display date or days
        int daysMask = mCursor.getInt(mCursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_DAYS));
        if (daysMask <= 0) {
            if (calendar.getTimeInMillis()
                    < Calendar.getInstance().getTimeInMillis() + DateUtils.DAY_IN_MILLIS) {
                timesAdapterViewHolder.mDateTextView.setText(mContext.getString(R.string.time_show_once));
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat(mContext.getString(R.string.date_pattern));
                timesAdapterViewHolder.mDateTextView.setText(dateFormat.format(date));
            }
        } else {

            // display days
            String displayDays = "";
            for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                if ((WakeUpDbHelper.getDayBitMask(day) & daysMask) > 0) {
                    if (!displayDays.isEmpty()) {
                        displayDays += "  ";
                    }
                    switch (day) {
                        case Calendar.SUNDAY:
                            displayDays += mContext.getString(R.string.add_time_sunday);
                            break;
                        case Calendar.MONDAY:
                            displayDays += mContext.getString(R.string.add_time_monday);
                            break;
                        case Calendar.TUESDAY:
                            displayDays += mContext.getString(R.string.add_time_tuesday);
                            break;
                        case Calendar.WEDNESDAY:
                            displayDays += mContext.getString(R.string.add_time_wednesday);
                            break;
                        case Calendar.THURSDAY:
                            displayDays += mContext.getString(R.string.add_time_thursday);
                            break;
                        case Calendar.FRIDAY:
                            displayDays += mContext.getString(R.string.add_time_friday);
                            break;
                        case Calendar.SATURDAY:
                            displayDays += mContext.getString(R.string.add_time_saturday);
                            break;
                    }
                }
            }
            timesAdapterViewHolder.mDateTextView.setText(displayDays.trim());
        }

        // display on/off button
        timesAdapterViewHolder.mAlarmStateButton.setChecked(
                mCursor.getInt(mCursor.getColumnIndex(WakeUpContract.WakeUpEntry.COLUMN_STATE))
                        == WakeUpContract.STATE_ALARM_ON);

        // alarm on/off button
        timesAdapterViewHolder.mAlarmStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Switch button = (Switch) view;
                String stringId = Integer.toString(currentId);

                // update mode
                ContentValues contentValues = new ContentValues();
                contentValues.put(WakeUpContract.WakeUpEntry.COLUMN_STATE,
                        button.isChecked() ? WakeUpContract.STATE_ALARM_ON : WakeUpContract.STATE_ALARM_OFF);
                Uri uri = WakeUpContract.WakeUpEntry.CONTENT_URI;
                uri = uri.buildUpon().appendPath(stringId).build();
                mContext.getContentResolver().update(uri,
                        contentValues,
                        null,
                        null );

                // actualize next alarm
                AlarmUtils.resetSnooze(mContext, currentId);
                if (button.isChecked()) {
                    AlarmUtils.toastNextAlarm(mContext, currentId);
                }
                AlarmUtils.startNextAlarm(mContext);

                if (button.isChecked()) {
                    timesAdapterViewHolder.itemView.setBackgroundColor(
                            mContext.getResources().getColor(R.color.colorTransparentBlue));
                } else {
                    timesAdapterViewHolder.itemView.setBackgroundColor(
                            mContext.getResources().getColor(R.color.colorTransparent));
                }
            }
        });

        if (timesAdapterViewHolder.mAlarmStateButton.isChecked()) {
            timesAdapterViewHolder.itemView.setBackgroundColor(
                    mContext.getResources().getColor(R.color.colorTransparentBlue));
        } else {
            timesAdapterViewHolder.itemView.setBackgroundColor(
                    mContext.getResources().getColor(R.color.colorTransparent));
        }

        // item tag for click event
        timesAdapterViewHolder.itemView.setTag(currentId);
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        }
        return mCursor.getCount();
    }

    public Cursor swapCursor(Cursor cursor) {
        if (mCursor == cursor) {
            return null;
        }
        Cursor returnCursor = mCursor;
        mCursor = cursor;
        if (cursor != null) {
            notifyDataSetChanged();
        }
        return returnCursor;
    }

    class TimesAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView mModeImageView;
        TextView mTimeTextView;
        TextView mNameTextView;
        TextView mDateTextView;
        Switch mAlarmStateButton;

        TimesAdapterViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            mModeImageView = (ImageView) view.findViewById(R.id.mode_image_view);
            mTimeTextView = (TextView) view.findViewById(R.id.list_time_view);
            mNameTextView = (TextView) view.findViewById(R.id.name_text_view);
            mDateTextView = (TextView) view.findViewById(R.id.date_text_view);
            mAlarmStateButton = (Switch) view.findViewById(R.id.list_alarm_state_button);
        }

        @Override
        public void onClick(View view) {
            mClickHandler.onClick(view, (int) view.getTag());
        }
    }
}