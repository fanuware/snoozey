package com.fanuware.android.snoop;

import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;


/**
 * customized grow button
 * user touch pinch open, grow button until event triggers
 */

public class GrowTouchButton extends android.support.v7.widget.AppCompatButton {
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
    private final float mScaleFactorThreshold = 2.0f;
    private int mBackground;

    public GrowTouchButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        // read configured background
        mBackground =((ColorDrawable)getBackground()).getColor();

        // remove existing background
        setBackground(null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(event);

        int action = (event.getAction() & MotionEvent.ACTION_MASK);
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP: {
                // see if button has triggered
                if (mScaleFactor > mScaleFactorThreshold) {
                    buttonTriggered();
                } else {
                    animateDefaultSize();
                }
                break;
            }
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // change bounds (increasing)
        Rect newRect = new Rect();
        canvas.getClipBounds(newRect);
        newRect.inset(
                -(int)((float)getWidth() * (mScaleFactor - 1f) / 2),
                -(int)((float)getHeight() * (mScaleFactor - 1f) / 2));
        canvas.clipRect(newRect, Region.Op.REPLACE);
        int strechedWidth = newRect.width();
        int strechedHeight = newRect.height();

        // draw circle in center
        Paint paint = new Paint();

        // border background circle
        int radius = Math.min(strechedWidth / 2, strechedHeight / 2);
        paint.setColor(ContextCompat.getColor(getContext(),
                mScaleFactor > mScaleFactorThreshold ? R.color.colorLight : R.color.alarmBackground));
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, paint);

        // draw vector image
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.art_cancel_button);
        drawable.setColorFilter(mBackground, PorterDuff.Mode.SRC_ATOP);
        Bitmap bitmap = Bitmap.createBitmap(strechedWidth, strechedHeight, Bitmap.Config.ARGB_8888);
        Canvas canvasBitmap = new Canvas(bitmap);
        drawable.setBounds(0, 0,
                canvasBitmap.getWidth(), canvasBitmap.getHeight());
        drawable.draw(canvasBitmap);
        canvas.drawBitmap(bitmap,
                (getWidth() / 2) - (strechedWidth / 2),
                (getHeight() / 2) - (strechedHeight / 2),
                paint);
    }

    // move back to default size
    private void animateDefaultSize() {
        ValueAnimator anim = ValueAnimator.ofFloat(mScaleFactor, 1f);
        anim.setEvaluator(new FloatEvaluator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mScaleFactor = (float) animation.getAnimatedValue();
                invalidate();
            } });
        anim.setDuration(200);
        anim.start();
    }

    // buttonTriggered
    private void buttonTriggered() {
        animateDefaultSize();
        callOnClick();
    }

    // listen expanding gesture
    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(1f, Math.min(mScaleFactor, 5.0f));
            invalidate();
            return true;
        }
    }
}
