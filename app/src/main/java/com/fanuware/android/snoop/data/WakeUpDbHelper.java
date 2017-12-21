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

package com.fanuware.android.snoop.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.fanuware.android.snoop.data.WakeUpContract.WakeUpEntry;

import java.util.Calendar;


public class WakeUpDbHelper extends SQLiteOpenHelper {

    // The name of the database
    private static final String DATABASE_NAME = "wakeUpDb.db";

    // If you change the database schema, you must increment the database version
    private static final int VERSION = 2;


    // Constructor
    WakeUpDbHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }


    /**
     * Called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create tasks table (careful to follow SQL formatting rules)
        final String CREATE_TABLE = "CREATE TABLE "  + WakeUpEntry.TABLE_NAME + " (" +
                WakeUpEntry._ID         + " INTEGER PRIMARY KEY, " +
                WakeUpEntry.COLUMN_TIME + " DATETIME NOT NULL, " +
                WakeUpEntry.COLUMN_STATE + " BOOLEAN NOT NULL, " +
                WakeUpEntry.COLUMN_MODE + " INTEGER NOT NULL," +
                WakeUpEntry.COLUMN_DAYS + " INTEGER NOT NULL, " +
                WakeUpEntry.COLUMN_NAME + " VARCHAR(20) NOT NULL, " +
                WakeUpEntry.COLUMN_SNOOZE_TIMESTAMP + " DATETIME NOT NULL);";
        db.execSQL(CREATE_TABLE);
    }


    /**
     * This method discards the old table of data and calls onCreate to recreate a new one.
     * This only occurs when the version number for this database (DATABASE_VERSION) is incremented.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + WakeUpEntry.TABLE_NAME);
        onCreate(db);
    }

    // convert calendar day bit mask, to store bit in database
    public static int getDayBitMask(int calendarDay) {
        switch (calendarDay) {
            case Calendar.SUNDAY:
                return 1;
            case Calendar.MONDAY:
                return 2;
            case Calendar.TUESDAY:
                return 4;
            case Calendar.WEDNESDAY:
                return 8;
            case Calendar.THURSDAY:
                return 16;
            case Calendar.FRIDAY:
                return 32;
            case Calendar.SATURDAY:
                return 64;
            default:
                throw new IllegalArgumentException("calendar day is invalid, id: " +calendarDay);
        }
    }
}
