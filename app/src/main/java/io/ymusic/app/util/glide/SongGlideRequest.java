package io.ymusic.app.util.glide;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.signature.MediaStoreSignature;

import io.ymusic.app.R;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.util.MusicUtil;
import io.ymusic.app.util.glide.audiocover.AudioFileCover;
import io.ymusic.app.util.glide.palette.BitmapPaletteTranscoder;
import io.ymusic.app.util.glide.palette.BitmapPaletteWrapper;

public class SongGlideRequest {

    public static final DiskCacheStrategy DEFAULT_DISK_CACHE_STRATEGY = DiskCacheStrategy.NONE;
    public static final int DEFAULT_ERROR_IMAGE = R.drawable.default_image;
    public static final int DEFAULT_ANIMATION = android.R.anim.fade_in;

    public static class Builder {
        final RequestManager requestManager;
        final Song song;
        boolean ignoreMediaStore;

        public static Builder from(@NonNull RequestManager requestManager, Song song) {
            return new Builder(requestManager, song);
        }

        private Builder(@NonNull RequestManager requestManager, Song song) {
            this.requestManager = requestManager;
            this.song = song;
        }

        public PaletteBuilder generatePalette(Context context) {
            return new PaletteBuilder(this, context);
        }

        public BitmapBuilder asBitmap() {
            return new BitmapBuilder(this);
        }

        public Builder ignoreMediaStore(boolean ignoreMediaStore) {
            this.ignoreMediaStore = ignoreMediaStore;
            return this;
        }

        public DrawableRequestBuilder<GlideDrawable> build() {
            return createBaseRequest(requestManager, song, ignoreMediaStore)
                    .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                    .error(DEFAULT_ERROR_IMAGE)
                    .animate(DEFAULT_ANIMATION)
                    .signature(createSignature(song));
        }
    }

    public static class BitmapBuilder {
        private final Builder builder;

        public BitmapBuilder(Builder builder) {
            this.builder = builder;
        }

        public BitmapRequestBuilder<?, Bitmap> build() {
            return createBaseRequest(builder.requestManager, builder.song, builder.ignoreMediaStore)
                    .asBitmap()
                    .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                    .error(DEFAULT_ERROR_IMAGE)
                    .animate(DEFAULT_ANIMATION)
                    .signature(createSignature(builder.song));
        }
    }

    public static class PaletteBuilder {
        final Context context;
        private final Builder builder;

        public PaletteBuilder(Builder builder, Context context) {
            this.builder = builder;
            this.context = context;
        }

        public BitmapRequestBuilder<?, BitmapPaletteWrapper> build() {
            return createBaseRequest(builder.requestManager, builder.song, builder.ignoreMediaStore)
                    .asBitmap()
                    .transcode(new BitmapPaletteTranscoder(context), BitmapPaletteWrapper.class)
                    .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                    .error(DEFAULT_ERROR_IMAGE)
                    .animate(DEFAULT_ANIMATION)
                    .signature(createSignature(builder.song));
        }
    }

    public static DrawableTypeRequest createBaseRequest(RequestManager requestManager, Song song, boolean ignoreMediaStore) {
        if (ignoreMediaStore) {
            return requestManager.load(new AudioFileCover(song.data));
        } else {
            return requestManager.loadFromMediaStore(MusicUtil.getMediaStoreAlbumCoverUri(song.albumId));
        }
    }

    public static Key createSignature(Song song) {
        return new MediaStoreSignature("", song.dateModified, 0);
    }
}
