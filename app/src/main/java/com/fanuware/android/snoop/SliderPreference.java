package com.fanuware.android.snoop;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.SeekBar;


public class SliderPreference extends Preference implements SeekBar.OnSeekBarChangeListener {
    boolean init = true;
    int mCurrentValue;
    SeekBar mVolumeSeekBar;
    String mSliderDefaultsValue = "0";
    int mSliderMin = 0;
    int mSliderMax = 100;
    int mSliderSteps = 1;
    String mUnit = "";
    int mTranslateValue1 = -1;
    String mTranslateString1 = "";

    public SliderPreference(Context context) {
        this(context, null, 0);
    }

    public SliderPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SliderPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_slider);

        // get attributes for slider
        String android = "http://schemas.android.com/apk/res/android";
        String custom = "http://schemas.android.com/apk/res/com.fanuware.android.snoop";
        mSliderDefaultsValue = getStringAttrValue(attrs, android, "defaultValue", "0");
        mSliderMin = attrs.getAttributeIntValue(android, "min", mSliderMin);
        mSliderMax = attrs.getAttributeIntValue(android, "max", mSliderMax);
        mSliderMax = attrs.getAttributeIntValue(android, "max", mSliderMax);
        mSliderSteps = attrs.getAttributeIntValue(custom, "steps", mSliderSteps);
        mUnit = getStringAttrValue(attrs, custom, "unit", mUnit);
        mTranslateValue1 = attrs.getAttributeIntValue(custom, "translate_value1", mTranslateValue1);
        mTranslateString1 = getStringAttrValue(attrs, custom, "translate_string1", mTranslateString1);
    }

    private String getStringAttrValue(AttributeSet attrs, String namespace, String attribute, String defaultValue)  {
        String value = attrs.getAttributeValue(namespace, attribute);
        if (value == null) {
            value = defaultValue;
        } else if (value.startsWith("@")) {
            int resId = Integer.parseInt(value.substring(1));
            value = getContext().getString(resId);
        }
        return value;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mVolumeSeekBar = (SeekBar) holder.findViewById(R.id.volumeSeekBar);
        if (init) {
            init = false;
            mVolumeSeekBar.setProgress((mCurrentValue - mSliderMin) * 100 / (mSliderMax - mSliderMin));
            mVolumeSeekBar.setOnSeekBarChangeListener(this);
        }
        if (mCurrentValue == 0) {
            mVolumeSeekBar.setThumb(getContext().getDrawable(R.drawable.seekbar_thumb_off));
        } else {
            mVolumeSeekBar.setThumb(getContext().getDrawable(R.drawable.seekbar_thumb_on));
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        // store in preference
        persistString(Integer.toString(i * (mSliderMax - mSliderMin) / 100 + mSliderMin));
        notifyChanged();
    }

    @Override
    public void setSummary(CharSequence summary) {
        try {
            mCurrentValue = Integer.parseInt(summary.toString());
        } catch (Exception e) {
            mCurrentValue = Integer.parseInt(mSliderDefaultsValue);
        }
        String summaryString;
        if ( mCurrentValue == mTranslateValue1) {
            summaryString = mTranslateString1;
        } else {
            summaryString = Integer.toString(mCurrentValue);
        }
        super.setSummary(summaryString + mUnit);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}