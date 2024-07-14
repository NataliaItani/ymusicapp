package io.ymusic.app.player.event;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.Arrays;
import java.util.List;

import io.ymusic.app.R;

public class CustomBottomSheetBehavior extends BottomSheetBehavior<FrameLayout> {

    public CustomBottomSheetBehavior(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    Rect globalRect = new Rect();
    private boolean skippingInterception = false;
    private final List<Integer> skipInterceptionOfElements = Arrays.asList(
            R.id.queue_bottom_sheet, R.id.queue_bottom_sheet_content, R.id.playPauseButton,
            R.id.playPreviousButton, R.id.playNextButton);

    @Override
    public boolean onInterceptTouchEvent(@NonNull final CoordinatorLayout parent, @NonNull final FrameLayout child, final MotionEvent event) {
//        // Drop following when action ends
//        if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
//            skippingInterception = false;
//        }
//
//        // Found that user still swiping, continue following
//        if (skippingInterception || getState() == BottomSheetBehavior.STATE_SETTLING) {
//            return false;
//        }
//
//        // Don't need to do anything if bottomSheet isn't expanded
//        if (getState() == BottomSheetBehavior.STATE_EXPANDED && event.getAction() == MotionEvent.ACTION_DOWN) {
//            // Without overriding scrolling will not work when user touches these elements
//            for (final Integer element : skipInterceptionOfElements) {
//                final View view = child.findViewById(element);
//                if (view != null) {
//                    final boolean visible = view.getGlobalVisibleRect(globalRect);
//                    if (visible && globalRect.contains((int) event.getRawX(), (int) event.getRawY())) {
//                        skippingInterception = true;
//                        return false;
//                    }
//                }
//            }
//        }
//
//        return super.onInterceptTouchEvent(parent, child, event);
        return false;
    }
}
