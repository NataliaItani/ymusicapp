package io.ymusic.app.util.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

public class LockableViewPager extends ViewPager {

    // true if we can swipe
    // false if we cannot swipe
    private boolean swipeEnable = false;

    public LockableViewPager(@NonNull Context context) {
        super(context);
    }

    public LockableViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSwipeEnabled(boolean enabled) {
        swipeEnable = enabled;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        // if we can swipe pass the event to the superclass
        // only continue to handle the touch event if swipe enabled
        return (swipeEnable) && (super.onTouchEvent(ev));
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Don't do anything with intercepted touch events if
        // we are not swipe
        try {
            return (swipeEnable) && (super.onInterceptTouchEvent(ev));
        } catch (Exception e) {
            return true;
        }
    }
}
