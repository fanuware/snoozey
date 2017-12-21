package com.fanuware.android.snoop;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;


/**
 * Created by fabian nussbaumer on 16.01.2018.
 */

public class AlarmStandardFragment extends Fragment {

    private boolean mSnoozeEnabled;
    private ImageView mAlarmResetAnimationView;
    private OnTriggerListener mOnTriggerListener;

    public interface OnTriggerListener {
        void onTrigger();
    }

    public void setOnTriggerListener(OnTriggerListener onTriggerListener) {
        mOnTriggerListener = onTriggerListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.alarm_cancel_standard, container, false);

        // Alarm reset touch button
        final Button alarmResetButton = (Button) view.findViewById(R.id.alarmResetButton);
        alarmResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnTriggerListener != null) {
                    mOnTriggerListener.onTrigger();
                }
            }
        });

        // button animation (raise, shrink)
        mAlarmResetAnimationView = (ImageView) view.findViewById(R.id.alarmResetAnimation);
        Animation multiAnim = AnimationUtils.loadAnimation(view.getContext(), R.anim.anim_alarm_reset);
        mAlarmResetAnimationView.startAnimation(multiAnim);

        // Inflate the layout for this fragment
        return view;
    }
}