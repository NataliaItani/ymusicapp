package io.ymusic.app.util.glide;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.kabouzeid.appthemehelper.util.ATHUtil;

import io.ymusic.app.R;
import io.ymusic.app.util.PhonographColorUtil;
import io.ymusic.app.util.glide.palette.BitmapPaletteTarget;
import io.ymusic.app.util.glide.palette.BitmapPaletteWrapper;

public abstract class PhonographColoredTarget extends BitmapPaletteTarget {

    public PhonographColoredTarget(ImageView view) {
        super(view);
    }

    @Override
    public void onLoadFailed(Exception e, Drawable errorDrawable) {
        super.onLoadFailed(e, errorDrawable);
        onColorReady(getDefaultFooterColor());
    }

    @Override
    public void onResourceReady(BitmapPaletteWrapper resource, GlideAnimation<? super BitmapPaletteWrapper> glideAnimation) {
        super.onResourceReady(resource, glideAnimation);
        onColorReady(PhonographColorUtil.getColor(resource.getPalette(), getDefaultFooterColor()));
    }

    protected int getDefaultFooterColor() {
        return ATHUtil.resolveColor(getView().getContext(), R.attr.defaultFooterColor);
    }

    public abstract void onColorReady(int color);
}
