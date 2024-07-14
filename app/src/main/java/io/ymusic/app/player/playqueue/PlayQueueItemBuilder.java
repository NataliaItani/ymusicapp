package io.ymusic.app.player.playqueue;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import io.ymusic.app.App;
import io.ymusic.app.util.GlideUtils;

public class PlayQueueItemBuilder {

    public interface OnSelectedListener {
        void selected(PlayQueueItem item, View view);
        void held(PlayQueueItem item, View view);
    }

    private OnSelectedListener onItemClickListener;

    public PlayQueueItemBuilder(final Context context) {
    }

    public void setOnSelectedListener(OnSelectedListener listener) {
        this.onItemClickListener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void buildStreamInfoItem(final PlayQueueItemHolder holder, final PlayQueueItem item) {

        if (!TextUtils.isEmpty(item.getTitle())) holder.itemVideoTitleView.setText(item.getTitle());
        holder.itemAdditionalDetailsView.setText(item.getUploader());
        GlideUtils.loadThumbnail(App.applicationContext, holder.itemThumbnailView, item.getThumbnailUrl());

        holder.itemRoot.setOnClickListener(view -> {
            if (onItemClickListener != null) {
                onItemClickListener.selected(item, view);
            }
        });

        holder.itemHandle.setOnClickListener(view -> {
            if (onItemClickListener != null) {
                onItemClickListener.held(item, view);
            }
        });
    }
}
