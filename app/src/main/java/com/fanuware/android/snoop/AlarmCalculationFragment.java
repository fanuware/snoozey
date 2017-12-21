package com.fanuware.android.snoop;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Random;

/**
 * Created by fabian nussbaumer on 16.01.2018.
 */

public class AlarmCalculationFragment extends Fragment {

    private OnTriggerListener mOnTriggerListener;
    private int mNumberA;
    private int mNumberB;
    private int mSolution;
    private String mOperator;
    TextView mCalculationTextView;
    NumberPicker mNumberPicker1;
    NumberPicker mNumberPicker2;

    public interface OnTriggerListener {
        void onTrigger();
    }

    public void setOnTriggerListener(OnTriggerListener onTriggerListener) {
        mOnTriggerListener = onTriggerListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.alarm_cancel_calculation, container, false);

        // calculate button
        Button calculateButton = (Button) view.findViewById(R.id.calculateButton);
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnTriggerListener != null) {
                    if (checkCalc()) {
                        mOnTriggerListener.onTrigger();
                    }
                }
            }
        });

        // number pickers
        mNumberPicker1 = (NumberPicker) view.findViewById(R.id.numberPicker1);
        mNumberPicker2 = (NumberPicker) view.findViewById(R.id.numberPicker2);
        mNumberPicker1.setMaxValue(9);
        mNumberPicker2.setMaxValue(9);
        mNumberPicker1.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                mSolution = (mSolution % 10) + i1 * 10;

                // set text
                mCalculationTextView.setText(getResources().getString(
                        R.string.calculation_text,
                        mNumberA, mOperator, mNumberB, mSolution)
                );
            }
        });
        mNumberPicker2.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                mSolution = (mSolution / 10) * 10 + i1;

                // set text
                mCalculationTextView.setText(getResources().getString(
                        R.string.calculation_text,
                        mNumberA, mOperator, mNumberB, mSolution)
                );
            }
        });

        // text to show calculation
        mCalculationTextView = (TextView) view.findViewById(R.id.calculationTextView);

        getRandomCalc();
        return view;
    }

    // check calculation
    private boolean checkCalc() {
        switch (mOperator) {
            case "+":
                return (mNumberA + mNumberB) == mSolution;
            case "-":
                return (mNumberA - mNumberB) == mSolution;
            case "×":
                return (mNumberA * mNumberB) == mSolution;
            case "/":
                return (mNumberA / mNumberB) == mSolution;
            default:
                return false;
        }
    }

    // generate random calculation to be solved
    private void getRandomCalc() {
        Random random = new Random(System.currentTimeMillis());
        switch (random.nextInt(4)) {
            case 0: // add
                mOperator = "+";
                do {
                    mNumberA = random.nextInt(99) + 1;
                    mNumberB = random.nextInt(99) + 1;
                } while ((mNumberA + mNumberB) > 99);
                break;
            case 1: // subtract
                mOperator = "-";
                do {
                    mNumberA = random.nextInt(99) + 1;
                    mNumberB = random.nextInt(99) + 1;
                } while ((mNumberA == mNumberB));
                if (mNumberA < mNumberB) {
                    int saveA = mNumberA;
                    mNumberA = mNumberB;
                    mNumberB = saveA;
                }
                break;
            case 2: // multiply
                mOperator = "×";
                mNumberA = random.nextInt(10) + 2;
                mNumberB = random.nextInt(10) + 2;
                break;
            default: // divide
                mOperator = "/";
                mNumberA = random.nextInt(10) + 2;
                mNumberB = random.nextInt(10) + 2;
                mNumberA = mNumberA * mNumberB;
        }
        mSolution = random.nextInt(100);

        // set text
        mCalculationTextView.setText(getResources().getString(
                R.string.calculation_text,
                mNumberA, mOperator, mNumberB, mSolution)
        );

        // set number pickers
        mNumberPicker1.setValue(mSolution / 10);
        mNumberPicker2.setValue(mSolution % 10);
    }
}