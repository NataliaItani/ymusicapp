package io.ymusic.app.util.glide.palette;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

import io.ymusic.app.util.PhonographColorUtil;

public class BitmapPaletteTranscoder implements ResourceTranscoder<Bitmap, BitmapPaletteWrapper> {

    private final BitmapPool bitmapPool;

    public BitmapPaletteTranscoder(Context context) {
        this(Glide.get(context).getBitmapPool());
    }

    public BitmapPaletteTranscoder(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
    }

    @Override
    public Resource<BitmapPaletteWrapper> transcode(Resource<Bitmap> bitmapResource) {
        Bitmap bitmap = bitmapResource.get();
        BitmapPaletteWrapper bitmapPaletteWrapper = new BitmapPaletteWrapper(bitmap, PhonographColorUtil.generatePalette(bitmap));
        return new BitmapPaletteResource(bitmapPaletteWrapper, bitmapPool);
    }

    @Override
    public String getId() {
        return "BitmapPaletteTranscoder.io.awesome.gagtube.util.glide.palette";
    }
}