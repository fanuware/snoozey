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

import android.net.Uri;
import android.provider.BaseColumns;


public class WakeUpContract {

    // mode
    public static final int STATE_ALARM_OFF = 0;
    public static final int STATE_ALARM_ON = 1;
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_AUTO_SNOOZE = 1;

    // snooze timestamp
    public static final String SNOOZE_TIMESTAMP_OFF = "snooze_off";

    // The authority, which is how your code knows which Content Provider to access
    public static final String AUTHORITY = "com.fanuware.android.snoop";

    // The base content URI = "content://" + <authority>
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // Define the possible paths for accessing data in this contract
    public static final String PATH_WAKEUP = "wakeup";

    /* TaskEntry is an inner class that defines the contents of the task table */
    public static final class WakeUpEntry implements BaseColumns {

        // content URI = base content URI + path
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_WAKEUP).build();


        // Task table and column names
        public static final String TABLE_NAME = "wakeup";

        // Since TaskEntry implements the interface "BaseColumns", it has an automatically produced
        // "_ID" column in addition to the two below
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_STATE = "state";
        public static final String COLUMN_MODE = "mode";
        public static final String COLUMN_DAYS = "days";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_SNOOZE_TIMESTAMP = "snooze_timestamp";


        /*

        wakeup
         - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        |  _id  |          time          |  name  |  state  |     mode        |  days (2^0: sunday .. 2^6: saturday)  |     lastSnooze
         - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        |  1   |  2017-12-22-10-16-00   |  work  |  on/off  |  MODE_DEFAULT  |               0b00100101              | 2017-12-22-10-16-00
         - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

         */

    }
}
