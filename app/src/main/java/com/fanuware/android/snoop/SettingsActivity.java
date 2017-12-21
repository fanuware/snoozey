package com.fanuware.android.snoop;

import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.kevalpatel.ringtonepicker.RingtonePickerDialog;
import com.kevalpatel.ringtonepicker.RingtonePickerListener;


/**
 * Created by fabian nussbaumer on 20.12.2017.
 */

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void selectRingtone(View view) {
        RingtonePickerDialog.Builder ringtonePickerBuilder = new RingtonePickerDialog.Builder(this, getSupportFragmentManager());

        // Set title of the dialog.
        // If set null, no title will be displayed.
        ringtonePickerBuilder.setTitle(getString(R.string.pref_alarm_ringtone_label));

        // Add the desirable ringtone types.
        // ringtonePickerBuilder.addRingtoneType(RingtonePickerDialog.Builder.TYPE_MUSIC);
        // ringtonePickerBuilder.addRingtoneType(RingtonePickerDialog.Builder.TYPE_NOTIFICATION);
        ringtonePickerBuilder.addRingtoneType(RingtonePickerDialog.Builder.TYPE_RINGTONE);
        // ringtonePickerBuilder.addRingtoneType(RingtonePickerDialog.Builder.TYPE_ALARM);

        // set current ringtone
        final SharedPreferences sharedPref = getSharedPreferences("snap", MODE_PRIVATE);
        String ringtoneUriString = sharedPref.getString("ringtone",
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
        Uri ringtoneUri = Uri.parse(ringtoneUriString);
        ringtonePickerBuilder.setCurrentRingtoneUri(ringtoneUri);

        // set the text to display of the positive (ok) button. (Optional)
        ringtonePickerBuilder.setPositiveButtonText(getString(R.string.pref_alarm_ringtone_ok));

        // set text to display as negative button. (Optional)
        ringtonePickerBuilder.setCancelButtonText(getString(R.string.pref_alarm_ringtone_cancel));

        // Set flag true if you want to play the com.ringtonepicker.sample of the clicked tone.
        ringtonePickerBuilder.setPlaySampleWhileSelection(true);

        // Set the callback listener.
        ringtonePickerBuilder.setListener(new RingtonePickerListener() {
            @Override
            public void OnRingtoneSelected(String ringtoneName, Uri ringtoneUri) {
                SharedPreferences.Editor prefEditor = sharedPref.edit();
                prefEditor.putString("ringtone", ringtoneUri.toString());
                prefEditor.commit();

                // retrieve
                String imageUriString = sharedPref.getString("ringtone", null);
                Uri imageUri = Uri.parse(imageUriString);
            }
        });

        //set the currently selected uri, to mark that ringtone as checked by default. (Optional)
        //ringtonePickerBuilder.setCurrentRingtoneUri(mCurrentSelectedUri);

        //Display the dialog.
        ringtonePickerBuilder.show();
    }
}
