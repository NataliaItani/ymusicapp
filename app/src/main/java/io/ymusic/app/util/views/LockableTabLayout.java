package io.ymusic.app.util.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.material.tabs.TabLayout;

public class LockableTabLayout extends TabLayout {

    public interface OnTouchEventInLockMode {
        void onTouchEventInLockMode(MotionEvent event);
    }

    private OnTouchEventInLockMode onTouchInLock;

    private boolean locked = false;

    public void setLocked(boolean value) {
        this.locked = value;
    }

    public void setOnTouchEventInLockMode(OnTouchEventInLockMode value) {
        this.onTouchInLock = value;
    }

    public LockableTabLayout(Context context) {
        super(context);
    }

    public LockableTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockableTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (locked && onTouchInLock != null) {
            onTouchInLock.onTouchEventInLockMode(event);
            return true;
        }
        return locked;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (locked && onTouchInLock != null) {
            onTouchInLock.onTouchEventInLockMode(event);
            return true;
        }
        return locked;
    }
}