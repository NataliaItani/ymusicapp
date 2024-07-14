package io.ymusic.app.util;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

import androidx.annotation.ColorInt;

public class ViewUtil {

    public final static int PHONOGRAPH_ANIM_TIME = 1000;

    public static Animator createTextColorTransition(final TextView v, @ColorInt final int startColor, @ColorInt final int endColor) {
        return createColorAnimator(v, "textColor", startColor, endColor);
    }

    private static Animator createColorAnimator(Object target, String propertyName, @ColorInt int startColor, @ColorInt int endColor) {
        ObjectAnimator animator;
        animator = ObjectAnimator.ofArgb(target, propertyName, startColor, endColor);

        animator.setInterpolator(new PathInterpolator(0.4f, 0f, 1f, 1f));
        animator.setDuration(PHONOGRAPH_ANIM_TIME);
        return animator;
    }

    public static boolean hitTest(View v, int x, int y) {
        final int tx = (int) (v.getTranslationX() + 0.5f);
        final int ty = (int) (v.getTranslationY() + 0.5f);
        final int left = v.getLeft() + tx;
        final int right = v.getRight() + tx;
        final int top = v.getTop() + ty;
        final int bottom = v.getBottom() + ty;

        return (x >= left) && (x <= right) && (y >= top) && (y <= bottom);
    }

    public static float convertDpToPixel(float dp, Resources resources) {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * metrics.density;
    }

    public static float convertPixelsToDp(float px, Resources resources) {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / metrics.density;
    }
}
