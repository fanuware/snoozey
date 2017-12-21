package com.fanuware.android.snoop;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.fanuware.android.snoop.data.WakeUpContract;
import com.fanuware.android.snoop.utils.AlarmUtils;

public class MainActivity extends AppCompatActivity implements TimesAdapter.TimesAdapterOnClickHandler{

    private TimesAdapter mTimesAdapter;
    private RecyclerView mTimesRecyclerView;
    private View mSelectedItemView;

    private FloatingActionButton mAddTimeFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.main_activity_title);
        final Activity activity = this;

        // add time button
        mAddTimeFab = (FloatingActionButton) findViewById(R.id.AddTimeFab);
        mAddTimeFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), AddTimeActivity.class);

                // transition
                ImageView imageView = (ImageView) view.findViewById(R.id.mode_image_view);
                Bundle bundle = ActivityOptions
                        .makeSceneTransitionAnimation(
                                activity,
                                new Pair<View, String>(view, "imageViewTransition"))
                        .toBundle();
                startActivity(intent, bundle);
            }
        });

        // recycler view to show all times
        mTimesRecyclerView = findViewById(R.id.recyclerview_times);

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mTimesRecyclerView.setLayoutManager(layoutManager);
        mTimesRecyclerView.setHasFixedSize(true);

        mTimesAdapter = new TimesAdapter(this, this);

        mTimesRecyclerView.setAdapter(mTimesAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
                return 0.2f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return super.getSwipeEscapeVelocity(defaultValue) * 10.0f;
            }

            // Called when a user swipes left or right on a ViewHolder
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int id = (int) viewHolder.itemView.getTag();

                // Build appropriate uri with String row id appended
                String stringId = Integer.toString(id);
                Uri uri = WakeUpContract.WakeUpEntry.CONTENT_URI;

                uri = uri.buildUpon().appendPath(stringId).build();

                getContentResolver().delete(uri, null, null);

                startSyncWakeUpList();
                AlarmUtils.startNextAlarm(getBaseContext());
            }
        }).attachToRecyclerView(mTimesRecyclerView);
        startSyncWakeUpList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSyncWakeUpList();

        if (mSelectedItemView != null) {
            mSelectedItemView.animate().alpha(1.0f).setDuration(100);
            mSelectedItemView = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent startFollowingActivity;
        switch(id) {
            case R.id.action_settings:
                startFollowingActivity = new Intent(this, SettingsActivity.class);
                startActivity(startFollowingActivity);
                return true;
            case R.id.action_about:
                startFollowingActivity = new Intent(this, AboutActivity.class);
                startActivity(startFollowingActivity);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    synchronized public void startSyncWakeUpList() {
        Cursor cursor = getContentResolver().query(
                WakeUpContract.WakeUpEntry.CONTENT_URI,
                null,
                null,
                null,
                null);
        mTimesAdapter.swapCursor(cursor);
    }

    @Override
    public void onClick(View view, int wakeUpId) {
        Intent intent = new Intent(getApplicationContext(), AddTimeActivity.class);
        intent.putExtra("WAKE_UP_ID", wakeUpId);

        // transition
        ImageView imageView = (ImageView) view.findViewById(R.id.mode_image_view);
        Bundle bundle = ActivityOptions
                .makeSceneTransitionAnimation(
                        this,
                        new Pair<View, String>((View)imageView, imageView.getTransitionName()))
                .toBundle();
        startActivity(intent, bundle);
        view.animate().alpha(0.0f).setDuration(500);
        mSelectedItemView = view;
    }
}
